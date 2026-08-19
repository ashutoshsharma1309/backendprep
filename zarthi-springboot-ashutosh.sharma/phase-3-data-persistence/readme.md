# Phase 3 — Data Persistence
### Prerequisite: Phase 2 complete. Branch: `git checkout -b phase-3-yourname`

**Catalog services this phase covers:** Spring Relational Persistence Service,
Spring MongoDB Persistence Service.

You'll replace Phase 2's in-memory `BookRepository` with real Spring Data
JPA persistence, and add a MongoDB-backed `Review` feature — a good fit for
document storage since a review's shape (rating, text, optional reply
thread) is naturally flexible.

---

## Module 1 — Spring Data JPA

### 1.1 Add the Dependency

In `project/pom.xml`, uncomment the JPA + H2 block (H2 is an in-memory
relational DB — perfect for training, zero setup):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

Add to `application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:librarydb
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```
`ddl-auto=update` auto-creates/updates tables from your entities — fine for
training, **never use this in real production** (Phase 9 explains why:
schema changes belong in reviewed, versioned migrations).

### 1.2 Turn `Book` Into an Entity

Replace the plain class from Phase 2,
`project/src/main/java/com/example/library/model/Book.java`:

```java
package com.example.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    private int publishedYear;

    public Book() {}

    public Book(String title, Author author, int publishedYear) {
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
    }

    // getters/setters for all fields, including author
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int y) { this.publishedYear = y; }
}
```

Create `project/src/main/java/com/example/library/model/Author.java`:

```java
package com.example.library.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();

    public Author() {}
    public Author(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Book> getBooks() { return books; }
}
```

`mappedBy = "author"` on `Author.books` tells JPA: "the foreign key lives on
the `Book` side (`author_id`), don't create a second join table for this
relationship." This is a `@OneToMany`/`@ManyToOne` bidirectional pair — the
single most common relationship mapping mistake is forgetting `mappedBy` and
ending up with an unwanted extra join table.

### 1.3 Real Repositories — Almost No Code Needed

Replace `BookRepository` entirely:

```java
package com.example.library.repository;

import com.example.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Derived query — Spring Data parses the method name and generates the query
    List<Book> findByAuthorNameIgnoreCase(String authorName);

    // Custom JPQL, for anything a derived name can't express cleanly
    @Query("SELECT b FROM Book b WHERE b.publishedYear > :year")
    List<Book> findPublishedAfter(int year);
}
```

Notice: this is an **interface**, no implementation. `JpaRepository<Book,
Long>` already gives you `save()`, `findById()`, `findAll()`,
`deleteById()`, `existsById()`, and more — the exact same method names
`BookController` was already calling in Phase 2, so **the controller needs
zero changes.** This is why the layered architecture (controller → service
→ repository) paid off: swapping the persistence implementation didn't
ripple upward.

**Your task this phase:** Create `AuthorRepository extends
JpaRepository<Author, Long>` the same way. Update `BookController`'s create
endpoint to look up (or create) an `Author` by name before saving a `Book` —
you'll need `AuthorRepository` injected into the controller (or better: into
a new `BookService`, see the note below).

> **Design note:** Phase 1 taught DI partly so that when you outgrow a
> controller doing everything, you can introduce a `BookService` between
> controller and repository without breaking anything. If your
> `BookController` is starting to feel crowded, now's a reasonable moment to
> extract a `BookService` — not required this phase, but worth trying.

### 1.4 The N+1 Problem

This query looks innocent:

```java
List<Author> authors = authorRepository.findAll();
for (Author a : authors) {
    System.out.println(a.getName() + ": " + a.getBooks().size());
}
```

By default, `@OneToMany` is **lazy** — `a.getBooks()` triggers a *separate*
query, per author, the first time it's accessed. For 50 authors, that's 1
query to fetch authors + 50 more queries for their books = 51 queries
(hence "N+1"). Enable SQL logging to see it happen:

```properties
spring.jpa.show-sql=true
```

**Fix it** with a fetch join when you know you'll need the books:

```java
@Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books")
List<Author> findAllWithBooks();
```

**Your task this phase:** reproduce the N+1 problem with logging on, count
the queries, then add and use `findAllWithBooks()` and confirm the count
drops to one query.

---

## Module 2 — Transactions (First Pass)

```java
package com.example.library.service;

import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public Book createBookWithAuthor(String title, String authorName, int year) {
        Author author = authorRepository.findAll().stream()
            .filter(a -> a.getName().equalsIgnoreCase(authorName))
            .findFirst()
            .orElseGet(() -> authorRepository.save(new Author(authorName)));

        Book book = new Book(title, author, year);
        return bookRepository.save(book);
    }
}
```

`@Transactional` here means: if `bookRepository.save()` fails after
`authorRepository.save()` already ran, **both roll back** — you won't end up
with an orphan `Author` and no `Book`. Prove it: temporarily add
`if (true) throw new RuntimeException("simulated failure");` right after
saving the author, call this method, then check the `authors` table (via
the H2 console at `/h2-console`) — it should be empty, not contain the
orphan author.

---

## Module 3 — Spring Data MongoDB (Reviews)

### 3.1 Add the Dependency

Uncomment in `project/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

Run a local MongoDB for training: `docker run -p 27017:27017 mongo:7`

### 3.2 The Document

Create `project/src/main/java/com/example/library/model/Review.java`:

```java
package com.example.library.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    private Long bookId;
    private String reviewerName;
    private int rating; // 1-5
    private String text;

    public Review() {}

    public Review(Long bookId, String reviewerName, int rating, String text) {
        this.bookId = bookId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.text = text;
    }

    public String getId() { return id; }
    public Long getBookId() { return bookId; }
    public String getReviewerName() { return reviewerName; }
    public int getRating() { return rating; }
    public String getText() { return text; }
}
```

Notice `Review` embeds everything about the review directly — no separate
"Reviewer" collection referenced by ID the way `Book`/`Author` are related
in JPA. That's a deliberate document-modeling choice: a review's reviewer
name doesn't need to stay in sync with a master reviewer record the way a
book's author does, so embedding is simpler and avoids a join-like lookup
for something read far more than it's updated.

### 3.3 The Repository

```java
package com.example.library.repository;

import com.example.library.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByBookId(Long bookId);
}
```

Same pattern as JPA — an interface, a derived query method, no
implementation to write.

**Your task this phase:** add a `ReviewController` with `POST /books/{bookId}/reviews`
and `GET /books/{bookId}/reviews`, following the same controller pattern
from Phase 2.

---

## Module 4 — Testing Persistence

```java
package com.example.library.repository;

import com.example.library.model.Author;
import com.example.library.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;

    @Test
    void findByAuthorName_returnsMatchingBooks() {
        Author author = authorRepository.save(new Author("Joshua Bloch"));
        bookRepository.save(new Book("Effective Java", author, 2018));

        List<Book> found = bookRepository.findByAuthorNameIgnoreCase("joshua bloch");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("Effective Java");
    }
}
```

`@DataJpaTest` spins up just the JPA slice against an in-memory H2 instance
— fast, isolated, no need for your real dev database.

---

## Phase 3 Completion Checklist

- [ ] `Book`/`Author` mapped as JPA entities with a correct bidirectional relationship
- [ ] `BookRepository`/`AuthorRepository` extend `JpaRepository`, controller unchanged
- [ ] Reproduced and fixed an N+1 query with a fetch join
- [ ] `BookService.createBookWithAuthor` demonstrates a working `@Transactional` rollback
- [ ] `Review` modeled as a MongoDB document with a working `ReviewRepository` and controller
- [ ] `@DataJpaTest` test passing for at least one derived query
- [ ] PR opened, CI green

**Next:** [Phase 4 — Talking to the Outside World](../phase-4-external-integrations/README.md)
