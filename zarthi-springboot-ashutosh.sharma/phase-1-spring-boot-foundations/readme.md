# Phase 1 — Spring Boot Foundations
### Prerequisite: Phase 0 complete

> **Repo note:** from here on, every phase edits `project/` — the real
> Library Service. Create a branch first: `git checkout -b phase-1-yourname`.

**Catalog services this phase covers:** Spring Configuration Service,
Spring Packaging Service, Spring Containerization Service.

---

## Module 1 — Spring Core: IoC and Dependency Injection

### 1.1 The Problem DI Solves

Without DI, objects build their own dependencies:

```java
public class BookService {
    private BookRepository repository = new BookRepository(); // tightly coupled
}
```

`BookService` now can't be tested without a real `BookRepository`, and can't
be reconfigured without editing this line. **Dependency Injection** flips
this: something else *hands* `BookService` its dependency.

```java
public class BookService {
    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }
}
```

Now a test can pass in a fake/mock `BookRepository`, and production code can
pass in the real one — `BookService` doesn't know or care which.

### 1.2 Letting Spring Do the Wiring

Spring's container (`ApplicationContext`) creates and wires your objects
(called **beans**) for you, based on annotations:

```java
package com.example.library.service;

import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final BookRepository repository;

    // Spring sees this constructor and automatically injects a
    // BookRepository bean here — no "new" anywhere.
    public BookService(BookRepository repository) {
        this.repository = repository;
    }
}
```

`@Service` marks this as a bean Spring should create and manage.
**Constructor injection** (as above) is preferred over field injection
(`@Autowired` directly on a field) because:
- The class can't be constructed in an invalid state (no repository = no object).
- You can unit test it with plain `new BookService(fakeRepository)` — no
  Spring container needed for the test at all.

**Your task this phase:** Create the package
`com.example.library.service` under `project/src/main/java/com/example/library/`.
You'll add a real `BookService` here in Phase 2 — for now, just create the
empty package (add a `.gitkeep` file if your tool won't track an empty
folder) so the structure exists.

### 1.3 Resolving Ambiguity

If two beans implement the same interface, Spring can't guess which to
inject:

```java
public interface NotificationSender { void send(String message); }

@Service
public class EmailSender implements NotificationSender { /* ... */ }

@Service
public class SmsSender implements NotificationSender { /* ... */ }
```

```java
@Service
public class OverdueNotifier {
    private final NotificationSender sender;

    // Spring will THROW an error here — ambiguous, two candidates
    public OverdueNotifier(NotificationSender sender) {
        this.sender = sender;
    }
}
```

Fix it with `@Qualifier` or by marking a default with `@Primary`:

```java
@Service
@Primary
public class EmailSender implements NotificationSender { /* ... */ }
```

```java
public OverdueNotifier(@Qualifier("smsSender") NotificationSender sender) {
    this.sender = sender;
}
```

You'll use this pattern for real in Phase 4 (Notifications).

---

## Module 2 — Configuration Management

### 2.1 Profiles

Right now, `project/src/main/resources/application.properties` has one flat
set of values. Real applications behave differently per environment
(different DB, different log level, different external API URLs). Spring
Profiles solve this.

**Your task this phase:**

1. Rename the strategy: keep `application.properties` for values common to
   *every* environment, and add two new files:

   `project/src/main/resources/application-dev.properties`:
   ```properties
   library.environment=dev
   logging.level.com.example.library=DEBUG
   ```

   `project/src/main/resources/application-prod.properties`:
   ```properties
   library.environment=prod
   logging.level.com.example.library=WARN
   ```

2. Activate a profile by running with:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   or by setting `spring.profiles.active=dev` in `application.properties`.

3. Confirm the log level actually changes between profiles — add a
   `log.debug(...)` line somewhere temporary and watch it appear in `dev`
   but not `prod`.

### 2.2 Typed Configuration with `@ConfigurationProperties`

Scattering `@Value("${library.environment}")` across many classes gets messy
fast once you have more than 2–3 related properties. Group them:

```properties
# application.properties
library.borrow.max-books-per-patron=5
library.borrow.loan-period-days=14
```

```java
package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "library.borrow")
public class BorrowProperties {
    private int maxBooksPerPatron;
    private int loanPeriodDays;

    public int getMaxBooksPerPatron() { return maxBooksPerPatron; }
    public void setMaxBooksPerPatron(int v) { this.maxBooksPerPatron = v; }
    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int v) { this.loanPeriodDays = v; }
}
```

Now any bean can `@Autowired`/constructor-inject `BorrowProperties` and get
typed, IDE-autocompletable access instead of scattered string keys. You'll
wire this into real borrowing logic in Phase 6 — for now, just create this
class and confirm the app still starts with it present (an unused
`@Component` is harmless).

**Precedence to know:** command-line arg > environment variable > profile-
specific file > `application.properties`. If a value seems "wrong," check
all four places before assuming the code is broken.

---

## Module 3 — Packaging

**Your task this phase:**

```bash
cd project
mvn clean package
ls target/*.jar
java -jar target/library-service-0.1.0.jar
```

Confirm the app starts from the JAR the same way it does from your IDE.
Inspect the JAR's contents:

```bash
jar tf target/library-service-0.1.0.jar | head -30
```

You'll see your compiled classes *and* every dependency's classes bundled
in — this is what makes it runnable standalone with just `java -jar`,
unlike a traditional WAR that needs an external app server.

---

## Module 4 — Containerizing the Application

### 4.1 A First Dockerfile

Create `project/Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/library-service-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run it:

```bash
mvn clean package
docker build -t library-service:0.1.0 .
docker run -p 8080:8080 library-service:0.1.0
curl http://localhost:8080/hello   # if you still have Phase 0's endpoint
```

### 4.2 Multi-Stage Build (Do This Instead)

The Dockerfile above requires you to `mvn package` locally first, and it
copies your build tool into the discussion unnecessarily. A **multi-stage
build** does the Maven build *inside* Docker, then throws away everything
except the final JAR:

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/library-service-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Your task this phase:** replace your Dockerfile with this multi-stage
version. Build both versions and compare `docker images` output for size —
the multi-stage image should be noticeably smaller (no Maven, no source
files, no `~/.m2` cache in the final image).

### 4.3 Passing Config into the Container

```bash
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev library-service:0.1.0
```

Spring Boot automatically maps `SPRING_PROFILES_ACTIVE` to
`spring.profiles.active` — any property can be overridden this way by
uppercasing and replacing `.`/`-` with `_`. Confirm your `dev` profile's
`DEBUG` logging shows up in the container logs when you set this.

---

## Phase 1 Completion Checklist

- [ ] Can explain constructor injection and why it's preferred over field injection
- [ ] Have created `application-dev.properties` / `application-prod.properties` and confirmed profile switching works
- [ ] Have created `BorrowProperties` using `@ConfigurationProperties`
- [ ] Can package `project/` into a runnable JAR and run it standalone
- [ ] Have a working multi-stage `Dockerfile` for `project/` and can explain why it's smaller
- [ ] Can override the active profile inside a container via an environment variable
- [ ] Opened a PR for this phase's branch, and CI (`.github/workflows/ci.yml`) is green

**Next:** [Phase 2 — Building REST APIs](../phase-2-rest-apis/README.md)
