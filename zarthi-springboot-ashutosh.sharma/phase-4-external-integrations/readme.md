# Phase 4 — Talking to the Outside World
### Prerequisite: Phase 3 complete. Branch: `git checkout -b phase-4-yourname`

**Catalog services:** External API Integration (Senior tier), Notifications,
File Storage, Application Caching (Senior tier). Pair with a mentor on the
Senior-tier pieces — implement them, but get them reviewed before merging.

---

## Module 1 — Calling External APIs

### 1.1 Add `WebClient`

`WebClient` needs the reactive web starter:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```
(You can use `WebClient` in a non-reactive app just fine — you don't need
the rest of WebFlux to use its HTTP client.)

### 1.2 An ISBN Lookup Client

Create `project/src/main/java/com/example/library/client/IsbnLookupClient.java`:

```java
package com.example.library.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Component
public class IsbnLookupClient {

    private final WebClient webClient;

    public IsbnLookupClient(WebClient.Builder builder) {
        this.webClient = builder
            .baseUrl("https://openlibrary.org")
            .build();
    }

    public IsbnLookupResult lookup(String isbn) {
        try {
            return webClient.get()
                .uri("/isbn/{isbn}.json", isbn)
                .retrieve()
                .bodyToMono(IsbnLookupResult.class)
                .timeout(Duration.ofSeconds(3))
                .block();
        } catch (WebClientResponseException.NotFound e) {
            return null; // translate "not found" into a clean null, not a leaked exception
        } catch (Exception e) {
            throw new ExternalServiceException("ISBN lookup failed for " + isbn, e);
        }
    }
}
```

```java
package com.example.library.client;

public class IsbnLookupResult {
    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
```

```java
package com.example.library.client;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**The timeout is not optional.** Without `.timeout(Duration.ofSeconds(3))`,
a hung external service hangs *your* request thread indefinitely. Prove it
matters: temporarily point `baseUrl` at an address that will hang (e.g. a
non-routable IP like `http://10.255.255.1`), call `lookup()`, and confirm it
throws after ~3 seconds instead of hanging forever.

**Your task this phase:** wire `IsbnLookupClient` into `BookService` — add
an endpoint `GET /books/lookup/{isbn}` that calls it and returns the title,
translating a `null` result into a proper `404`.

---

## Module 2 — Notifications

```java
package com.example.library.notification;

public interface NotificationSender {
    void send(String to, String message);
}
```

```java
package com.example.library.notification;

import org.springframework.stereotype.Service;

@Service
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(String to, String message) {
        // Real implementation would call an email provider SDK/API.
        // For training, log it — the point is the calling contract, not the transport.
        System.out.println("EMAIL to " + to + ": " + message);
    }
}
```

```java
package com.example.library.notification;

import org.springframework.stereotype.Service;

@Service
public class OverdueNotifier {

    private final NotificationSender sender;

    public OverdueNotifier(NotificationSender sender) {
        this.sender = sender;
    }

    public void notifyOverdue(String patronEmail, String bookTitle) {
        try {
            sender.send(patronEmail, "Your book \"" + bookTitle + "\" is overdue.");
        } catch (Exception e) {
            // Decision made here: a failed notification does NOT fail the
            // caller's operation (e.g. marking a book overdue still succeeds
            // even if the email couldn't be sent). Log it for follow-up instead.
            System.err.println("Failed to notify " + patronEmail + ": " + e.getMessage());
        }
    }
}
```

**Your task this phase:** write down (as a comment or in your PR
description) *why* a failed notification shouldn't roll back the overdue
marking — what would go wrong if it did?

---

## Module 3 — File Storage (Book Covers)

Use `MultipartFile` for uploads. For training, store to local disk behind an
interface, so swapping to real S3-style storage later doesn't touch calling
code:

```java
package com.example.library.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String store(MultipartFile file);   // returns a retrievable key/URL
    byte[] retrieve(String key);
}
```

```java
package com.example.library.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root = Paths.get("uploaded-covers");

    public LocalFileStorage() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public String store(MultipartFile file) {
        if (file.getSize() > 5_000_000) {
            throw new IllegalArgumentException("File exceeds 5MB limit");
        }
        String key = UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), root.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        return key;
    }

    @Override
    public byte[] retrieve(String key) {
        try {
            return Files.readAllBytes(root.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }
}
```

**Note for later:** this local-disk implementation is fine for training but
doesn't scale past one instance (Phase 8/9 will touch on why). Swapping
`LocalFileStorage` for an S3-backed `FileStorage` implementation later
requires zero changes to whatever controller calls it — same interface
principle as Phase 3's repository swap.

**Your task this phase:** add `POST /books/{id}/cover` (accepts a
`MultipartFile`, calls `store()`) and `GET /books/{id}/cover` (calls
`retrieve()` and returns the bytes).

---

## Module 4 — Caching with Redis (Senior-tier — pair on this one)

### 4.1 Setup

Uncomment in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

Run Redis locally: `docker run -p 6379:6379 redis:7`

Enable caching on your main class:
```java
@SpringBootApplication
@EnableCaching
public class LibraryServiceApplication { /* ... */ }
```

### 4.2 Cache a Repeated Lookup

```java
@Service
public class BookService {

    @Cacheable(value = "popularBooks", key = "#id")
    public Book getPopularBook(Long id) {
        System.out.println("DB hit for book " + id); // should only print on the FIRST call per id
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @CacheEvict(value = "popularBooks", key = "#book.id")
    public Book updatePopularBook(Book book) {
        return bookRepository.save(book);
    }
}
```

```properties
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

**The bug this exists to prevent:** without `@CacheEvict` on the update
path, a caller could update a book and then keep receiving the *stale*
cached version from `getPopularBook()` for up to 10 minutes (the TTL).
**Your task this phase:** prove this two ways — call `getPopularBook()`
twice and confirm "DB hit" only logs once; then call `updatePopularBook()`
and `getPopularBook()` again, and confirm you get the *updated* data, not a
stale cached copy.

---

## Phase 4 Completion Checklist

- [ ] `IsbnLookupClient` implemented with a working timeout, proven to actually trigger
- [ ] External failures translated into your own exception type, not leaked raw
- [ ] `OverdueNotifier` implemented with a documented decision on failure handling
- [ ] Cover upload/download working with size validation
- [ ] `@Cacheable`/`@CacheEvict` implemented and proven correct (not serving stale data after an update)
- [ ] PR opened, reviewed by a mentor (this phase has Senior-tier work), CI green

**Next:** [Phase 5 — Securing the Application](../phase-5-security/README.md)
