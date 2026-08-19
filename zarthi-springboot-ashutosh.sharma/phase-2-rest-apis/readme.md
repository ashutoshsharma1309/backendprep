# Phase 2 — Building REST APIs
### Prerequisite: Phase 1 complete. Branch: `git checkout -b phase-2-yourname`

**Catalog service this phase covers:** Spring REST API Service — *Implement REST APIs*.

By the end of this phase, `project/` has a working CRUD API for `Book`.

---

## Module 1 — Controllers and Routing

### 1.1 Your First Real Resource

Create `project/src/main/java/com/example/library/model/Book.java`:

```java
package com.example.library.model;

public class Book {
    private Long id;
    private String title;
    private String author;
    private int publishedYear;

    public Book() {}

    public Book(Long id, String title, String author, int publishedYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int y) { this.publishedYear = y; }
}
```

(This is a plain in-memory model for now — real persistence comes in Phase 3.)

### 1.2 A Repository Stand-In

Until Phase 3 wires up a real database, use an in-memory store so the API is
testable end to end today. Create
`project/src/main/java/com/example/library/repository/BookRepository.java`:

```java
package com.example.library.repository;

import com.example.library.model.Book;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {

    private final Map<Long, Book> store = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(idGenerator.getAndIncrement());
        }
        store.put(book.getId(), book);
        return book;
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public boolean existsById(Long id) {
        return store.containsKey(id);
    }
}
```

This is a real Spring bean (`@Repository`) — you'll swap its internals for
JPA in Phase 3 without changing anything that *calls* it, because the method
signatures stay the same. That's the point of the layered architecture.

### 1.3 The Controller

Create `project/src/main/java/com/example/library/controller/BookController.java`:

```java
package com.example.library.controller;

import com.example.library.model.Book;
import com.example.library.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Book> listBooks(@RequestParam(required = false) String author) {
        List<Book> books = repository.findAll();
        if (author != null) {
            books.removeIf(b -> !b.getAuthor().equalsIgnoreCase(author));
        }
        return books;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book saved = repository.save(book);
        return ResponseEntity.created(URI.create("/books/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        book.setId(id);
        return ResponseEntity.ok(repository.save(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Run it and try every endpoint:**

```bash
mvn spring-boot:run

curl -X POST localhost:8080/books -H "Content-Type: application/json" \
    -d '{"title":"Effective Java","author":"Joshua Bloch","publishedYear":2018}'
curl localhost:8080/books
curl localhost:8080/books/1
curl -X PUT localhost:8080/books/1 -H "Content-Type: application/json" \
    -d '{"title":"Effective Java (3rd Ed)","author":"Joshua Bloch","publishedYear":2018}'
curl -X DELETE localhost:8080/books/1
curl -i localhost:8080/books/1   # should now be 404
```

Confirm every status code matches what Phase 0's HTTP module taught you to
expect (`201` on create with a `Location` header, `204` on delete, `404` for
a missing book).

---

## Module 2 — DTOs and Request Validation

### 2.1 Why Not Just Use `Book` Directly?

Right now `createBook` accepts a raw `Book`, including its `id` field — a
caller could set `id` themselves, which is wrong (the server should assign
it). A **DTO** (Data Transfer Object) fixes this by describing exactly what
the API accepts, separately from your internal model.

Create `project/src/main/java/com/example/library/dto/BookRequest.java`:

```java
package com.example.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class BookRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "author is required")
    private String author;

    @Min(value = 1450, message = "publishedYear must be a plausible year")
    private int publishedYear;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int y) { this.publishedYear = y; }
}
```

### 2.2 Wiring It In

Update `createBook` in `BookController`:

```java
@PostMapping
public ResponseEntity<Book> createBook(@Valid @RequestBody BookRequest request) {
    Book book = new Book(null, request.getTitle(), request.getAuthor(), request.getPublishedYear());
    Book saved = repository.save(book);
    return ResponseEntity.created(URI.create("/books/" + saved.getId())).body(saved);
}
```

`@Valid` tells Spring to run the Bean Validation annotations before the
method body even runs. Try posting an invalid body:

```bash
curl -i -X POST localhost:8080/books -H "Content-Type: application/json" -d '{}'
```

You'll get a `400` — but with Spring's *default* error format, which is
verbose and not very caller-friendly. Module 3 fixes that.

**Your task this phase:** update `updateBook` to use `BookRequest` too,
for the same reason (don't let a caller set `id` via the body).

---

## Module 3 — Consistent Error Responses

### 3.1 A Custom Exception

Create `project/src/main/java/com/example/library/exception/BookNotFoundException.java`:

```java
package com.example.library.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("No book found with id: " + id);
    }
}
```

Update the controller to throw it instead of manually building 404
responses:

```java
@GetMapping("/{id}")
public Book getBook(@PathVariable Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));
}
```

### 3.2 A Structured Error Body

Create `project/src/main/java/com/example/library/exception/ErrorResponse.java`:

```java
package com.example.library.exception;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {
    private final String message;
    private final Instant timestamp = Instant.now();
    private List<String> details;

    public ErrorResponse(String message) { this.message = message; }
    public ErrorResponse(String message, List<String> details) {
        this.message = message;
        this.details = details;
    }

    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public List<String> getDetails() { return details; }
}
```

### 3.3 Handling It Globally

Create `project/src/main/java/com/example/library/exception/GlobalExceptionHandler.java`:

```java
package com.example.library.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Never leak ex.getMessage() or a stack trace to the caller here —
        // log it server-side, return something generic to the client.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred"));
    }
}
```

**@RestControllerAdvice** applies these handlers globally, to every
controller — you write the mapping once, not per-controller.

**Try it:**
```bash
curl -i localhost:8080/books/999          # structured 404
curl -i -X POST localhost:8080/books -H "Content-Type: application/json" -d '{}'  # structured 400 with field details
```

---

## Module 4 — Testing the API

Create `project/src/test/java/com/example/library/controller/BookControllerTest.java`:

```java
package com.example.library.controller;

import com.example.library.model.Book;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookRepository repository;

    @Test
    void getBook_returns200AndBody_whenBookExists() throws Exception {
        Book book = new Book(1L, "Effective Java", "Joshua Bloch", 2018);
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        mockMvc.perform(get("/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    void getBook_returns404_whenBookMissing() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/books/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createBook_returns400_whenTitleMissing() throws Exception {
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"author\":\"Someone\",\"publishedYear\":2020}"))
            .andExpect(status().isBadRequest());
    }
}
```

Run `mvn test` and confirm all three pass. `@WebMvcTest` loads only the web
layer (not your whole application context), and `@MockBean` swaps in a fake
`BookRepository` — this is why these tests run in milliseconds, not seconds.

---

## Phase 2 Completion Checklist

- [ ] Full CRUD `BookController` implemented and manually tested with `curl`
- [ ] `BookRequest` DTO created with validation, wired into create and update
- [ ] `BookNotFoundException` + `GlobalExceptionHandler` returning structured errors
- [ ] `MockMvc` tests covering success, not-found, and validation-failure paths, all passing
- [ ] PR opened for this phase, CI green

**Next:** [Phase 3 — Data Persistence](../phase-3-data-persistence/README.md)
