# Phase 0 — Prerequisites: The Full Training Document
### Spring Boot Fresher Onboarding

> **Repo note:** this phase is pure practice — you won't touch `project/`
> yet. Do every "Try it yourself" inline in this doc and in
> [`exercises/`](./exercises), in a scratch folder or scratch repo of your
> own. `project/` (the real Library Service) starts in **Phase 1**.

Welcome. This document is meant to be read and worked through top to bottom —
you shouldn't need anyone standing over your shoulder to get through it. Type
out every code example yourself (don't copy-paste); typing builds muscle
memory that copy-paste doesn't.

---

## Module 1 — Core Java

### 1.1 Objects, Classes, and Why We Bother

A **class** is a blueprint. An **object** is a thing built from that blueprint.
If you've never written OOP code before, here's the smallest possible example:

```java
public class Book {
    private String title;
    private String author;
    private int publishedYear;

    public Book(String title, String author, int publishedYear) {
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    @Override
    public String toString() {
        return title + " by " + author + " (" + publishedYear + ")";
    }
}
```

Notice: the fields (`title`, `author`, `publishedYear`) are `private`. That's
**encapsulation** — the outside world can't reach in and change a `Book`'s
title directly; it has to go through methods you've deliberately exposed
(here, just getters — this `Book` is immutable once created, which is
generally a good default). This is not bureaucracy for its own sake: it means
you can change how `Book` stores its data internally later without breaking
every piece of code that uses it, as long as the public methods still behave
the same way.

**Try it yourself:** Create a `main` method that builds three `Book` objects
and prints them using `toString()`.

```java
public class Main {
    public static void main(String[] args) {
        Book b1 = new Book("Effective Java", "Joshua Bloch", 2018);
        Book b2 = new Book("Clean Code", "Robert Martin", 2008);
        System.out.println(b1);
        System.out.println(b2);
    }
}
```

### 1.2 Inheritance and Interfaces

**Inheritance** lets one class reuse and extend another's behavior. Use it
sparingly — it creates a tight coupling between classes that's hard to undo
later. A good rule of thumb: inherit when there's a genuine "is-a"
relationship, not just to avoid retyping a few fields.

```java
public abstract class Media {
    protected String title;

    public Media(String title) {
        this.title = title;
    }

    public abstract String describe();
}

public class Book extends Media {
    private String author;

    public Book(String title, String author) {
        super(title);
        this.author = author;
    }

    @Override
    public String describe() {
        return title + " by " + author;
    }
}
```

`Media` is `abstract` — you can never do `new Media(...)`, only extend it.
`describe()` has no body in `Media`; every subclass is *forced* to provide one.

**Interfaces** are a different tool for a different job: they describe
*capability*, not identity. A class can implement many interfaces but extend
only one class.

```java
public interface Borrowable {
    void borrow(String borrowerName);
    void returnItem();
}

public class Book extends Media implements Borrowable {
    private boolean isBorrowed = false;

    // ...constructor, describe() as before...

    @Override
    public void borrow(String borrowerName) {
        if (isBorrowed) {
            throw new IllegalStateException("Already borrowed");
        }
        isBorrowed = true;
        System.out.println(title + " borrowed by " + borrowerName);
    }

    @Override
    public void returnItem() {
        isBorrowed = false;
    }
}
```

**When to choose which:** ask "is this a *kind of* X, sharing X's actual
implementation?" → inheritance. Ask "can this *do* Y, regardless of what it
otherwise is?" → interface. A `Book` and a `DVD` are both `Media` (inheritance
makes sense) and both `Borrowable` (a capability, not an identity — interface
makes sense).

**Try it yourself:** Create a `DVD` class that also extends `Media` and
implements `Borrowable`, with its own `describe()`. Build a `List<Media>`
containing both a `Book` and a `DVD`, and loop over it calling `describe()`
on each — this is polymorphism: the same method call runs different code
depending on the actual object type.

### 1.3 Collections

You'll use three collection types constantly:

- **`List<T>`** (usually `ArrayList`) — ordered, allows duplicates, indexed
  access.
- **`Set<T>`** (usually `HashSet`) — no duplicates, no guaranteed order.
- **`Map<K, V>`** (usually `HashMap`) — key-value pairs, no duplicate keys.

```java
List<Book> books = new ArrayList<>();
books.add(new Book("Effective Java", "Joshua Bloch", 2018));
books.add(new Book("Clean Code", "Robert Martin", 2008));

Set<String> authors = new HashSet<>();
authors.add("Joshua Bloch");
authors.add("Joshua Bloch"); // silently ignored, sets dedupe

Map<String, Book> booksByTitle = new HashMap<>();
booksByTitle.put("Effective Java", books.get(0));
Book found = booksByTitle.get("Effective Java");
```

**Try it yourself:** Build a `Map<String, List<Book>>` grouping books by
author (a real library might have several books per author). Populate it by
looping over a `List<Book>` and adding each book to the list under its
author's key, creating the list the first time you see a new author.

### 1.4 Exceptions

Java has two kinds of exceptions:

- **Checked exceptions** (extend `Exception`, not `RuntimeException`) — the
  compiler *forces* you to either catch them or declare `throws`. Use these
  for conditions a caller can reasonably be expected to recover from.
- **Unchecked exceptions** (extend `RuntimeException`) — no compiler
  enforcement. Use these for programming errors that shouldn't normally
  happen (bad arguments, invalid state).

```java
public class BookNotFoundException extends Exception {
    public BookNotFoundException(String title) {
        super("No book found with title: " + title);
    }
}

public class Library {
    private Map<String, Book> books = new HashMap<>();

    public Book find(String title) throws BookNotFoundException {
        Book book = books.get(title);
        if (book == null) {
            throw new BookNotFoundException(title);
        }
        return book;
    }
}
```

Calling code must handle it:

```java
try {
    Book b = library.find("Nonexistent Title");
} catch (BookNotFoundException e) {
    System.out.println("Handled: " + e.getMessage());
}
```

**Try it yourself:** Write a `LibraryFullException extends RuntimeException`
for when a library reaches capacity, and a `BookNotFoundException extends
Exception` (checked) for a missing book. Write code that triggers both and
confirm the compiler complains about one but not the other if you don't
handle it.

### 1.5 Streams and Lambdas

A **stream** is a pipeline for processing collections declaratively — you
describe *what* you want, not the loop mechanics of *how* to get it.

```java
List<Book> books = List.of(
    new Book("Effective Java", "Joshua Bloch", 2018),
    new Book("Clean Code", "Robert Martin", 2008),
    new Book("Java Concurrency in Practice", "Brian Goetz", 2006)
);

// Filter + sort + collect
List<String> recentTitles = books.stream()
    .filter(b -> b.getPublishedYear() > 2007)
    .sorted((a, b) -> b.getPublishedYear() - a.getPublishedYear())
    .map(Book::getTitle)
    .collect(Collectors.toList());

// Join into a single string
String joined = books.stream()
    .map(Book::getTitle)
    .collect(Collectors.joining(", "));
```

Compare that to the equivalent loop-based version:

```java
List<String> recentTitles = new ArrayList<>();
for (Book b : books) {
    if (b.getPublishedYear() > 2007) {
        recentTitles.add(b.getTitle());
    }
}
recentTitles.sort((a, b) -> /* ... */);
```

Both work. The stream version is more common in modern Java codebases
because it reads closer to the intent ("filter, then collect titles") rather
than the mechanics.

**Try it yourself:** Given the `books` list above, use a stream to: find the
oldest book (`min` with a comparator), count how many books were published
before 2010, and build a comma-separated string of just the authors
(no duplicates — hint: combine with `Collectors.toSet()` first).

### 1.6 `equals()` and `hashCode()`

If you ever put an object into a `HashSet` or use it as a `HashMap` key, and
you want two *different objects* with the same data to be treated as equal,
you must override both `equals()` and `hashCode()` **together**:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Book)) return false;
    Book other = (Book) o;
    return publishedYear == other.publishedYear
        && title.equals(other.title)
        && author.equals(other.author);
}

@Override
public int hashCode() {
    return Objects.hash(title, author, publishedYear);
}
```

**Why both, together?** `HashMap`/`HashSet` use `hashCode()` first to find
the right "bucket," then `equals()` to confirm a match within that bucket.
If you override `equals()` but leave the default `hashCode()` (which is
based on memory address), two "equal" books can end up in different buckets
and a `HashSet` will treat them as different — silently. This is one of the
most common real-world Java bugs, and it doesn't throw an error; it just
quietly gives you wrong results.

**Try it yourself:** Override only `equals()` on `Book` (not `hashCode()`),
add two "equal" books to a `HashSet<Book>`, and print the set's size —
confirm it shows `2`, not `1`. Then add `hashCode()` and confirm it now
correctly shows `1`.

---

## Module 2 — Build Tooling: Maven

Maven answers: how do I download dependencies, compile code, run tests, and
package this into something runnable — consistently, on any machine?

### 2.1 Anatomy of `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>library-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- `groupId`/`artifactId`/`version` — your project's coordinates.
- `dependencies` — what your code needs. The `<scope>test</scope>` on JUnit
  means: only include this when compiling/running tests, not in the final
  shipped artifact.

### 2.2 The Build Lifecycle

Maven runs phases **in order**, each depending on the ones before it:

```
validate → compile → test → package → verify → install → deploy
```

Running `mvn package` doesn't just package — it runs `validate`, `compile`,
and `test` first, automatically, because `package` depends on them.

**Try it yourself:**
```bash
mvn archetype:generate -DgroupId=com.example -DartifactId=library-app \
    -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd library-app
mvn clean package
```
Look inside `target/` — you'll find the compiled `.class` files and a `.jar`.
Run `mvn clean` again and watch `target/` disappear — `clean` just deletes
build output, nothing else.

### 2.3 Dependency Scopes

- **`compile`** (default) — needed at compile time *and* runtime, included
  in the final artifact.
- **`provided`** — needed at compile time, but the runtime environment
  already provides it (e.g. a servlet container providing `servlet-api`).
- **`test`** — only needed for running tests.
- **`runtime`** — needed at runtime but not compile time (e.g. a JDBC
  driver, referenced only by string class name in config).

**Try it yourself:** Add a second dependency to your `pom.xml` (e.g.
`org.apache.commons:commons-lang3`), run `mvn dependency:tree`, and read the
output — you'll see your direct dependency plus any transitive ones it pulls
in.

---

## Module 3 — Git Fundamentals

### 3.1 The Mental Model

Git tracks **snapshots**, not diffs (even though it displays diffs). Every
commit is a full snapshot of your project at that point, linked to its
parent commit(s). A **branch** is just a movable pointer to a commit.

### 3.2 The Daily Flow

```bash
git clone https://github.com/example/library-app.git
cd library-app

git checkout -b feature/add-book-search   # create + switch to a new branch
# ...make changes...
git add src/main/java/com/example/BookSearch.java
git commit -m "Add search-by-author endpoint"

git push -u origin feature/add-book-search
# open a Pull Request on GitHub/GitLab from here
```

**A good commit message** explains *why*, not just *what* (the diff already
shows what). Compare:
- Bad: `"fix bug"`
- Good: `"Fix NPE when searching with a null author filter"`

### 3.3 Merge Conflicts, For Real

A conflict happens when Git can't automatically reconcile two branches'
changes to the same lines. Say `main` and your branch both changed this method:

```java
<<<<<<< HEAD
public List<Book> search(String author) {
    return books.stream()
        .filter(b -> b.getAuthor().equalsIgnoreCase(author))
        .collect(Collectors.toList());
}
=======
public List<Book> search(String author) {
    if (author == null) return List.of();
    return books.stream()
        .filter(b -> b.getAuthor().equals(author))
        .collect(Collectors.toList());
}
>>>>>>> feature/add-book-search
```

**Don't just pick one side.** Read both — `HEAD` (top, the branch you're
merging *into*) added nothing new here, but the incoming branch (bottom)
added a null-check, and lost case-insensitivity. The correct resolution
combines both intents:

```java
public List<Book> search(String author) {
    if (author == null) return List.of();
    return books.stream()
        .filter(b -> b.getAuthor().equalsIgnoreCase(author))
        .collect(Collectors.toList());
}
```

After resolving:
```bash
git add src/main/java/com/example/BookSearch.java
git commit          # completes the merge
mvn test            # ALWAYS verify after resolving — a clean merge can still be logically broken
```

**Try it yourself:** With a partner (or by simulating it alone with two
local branches), create exactly this kind of conflict — both branches
modifying the same method differently — and resolve it, combining both
changes correctly rather than discarding either one.

### 3.4 Merge vs. Rebase (Just Enough to Know)

- `git merge feature-branch` — creates a new "merge commit" joining two
  histories. Preserves exact history, including the fact that branches diverged.
- `git rebase main` — replays your branch's commits on top of `main`,
  producing a linear history. Cleaner-looking log, but rewrites commit hashes
  — **never rebase a branch other people are also working on.**

Most teams standardize on one for consistency. Ask your team which — don't
mix strategies on a shared branch without knowing why.

---

## Module 4 — HTTP Fundamentals

### 4.1 Anatomy of a Request/Response

A request:
```
POST /books HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOi...

{"title": "Effective Java", "author": "Joshua Bloch"}
```

A response:
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: /books/42

{"id": 42, "title": "Effective Java", "author": "Joshua Bloch"}
```

### 4.2 Methods and What They *Mean*

| Method | Meaning | Idempotent? |
|---|---|---|
| `GET` | Retrieve a resource, no side effects | Yes |
| `POST` | Create a resource / trigger an action | No |
| `PUT` | Replace a resource entirely | Yes |
| `PATCH` | Partially update a resource | Not guaranteed |
| `DELETE` | Remove a resource | Yes |

**Idempotent** means: calling it once or ten times with the same input has
the same effect as calling it once. `DELETE /books/42` called twice still
results in book 42 being gone — same end state. `POST /books` called twice
creates *two* books — different end state, so `POST` is not idempotent. This
distinction matters a lot once you deal with retries (a client that doesn't
know if its request succeeded should generally be safe to retry idempotent
calls, but not `POST` calls — this comes back in a big way later, in the
idempotency-key training).

### 4.3 Status Codes Worth Memorizing

| Code | Meaning | When |
|---|---|---|
| `200` | OK | Successful `GET`/`PUT`/`PATCH` |
| `201` | Created | Successful `POST` that created something |
| `204` | No Content | Successful `DELETE`, nothing to return |
| `400` | Bad Request | Malformed/invalid input |
| `401` | Unauthorized | No valid credentials provided at all |
| `403` | Forbidden | Credentials are valid, but not allowed to do this |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Request conflicts with current state (e.g. duplicate) |
| `500` | Internal Server Error | Something broke on the server, not the client's fault |

**Try it yourself:** Using `curl` or Postman, hit a free public test API
(e.g. `https://jsonplaceholder.typicode.com/posts`):
```bash
curl -i https://jsonplaceholder.typicode.com/posts/1
curl -i https://jsonplaceholder.typicode.com/posts/999999   # doesn't exist
curl -i -X POST https://jsonplaceholder.typicode.com/posts \
    -H "Content-Type: application/json" \
    -d '{"title": "test", "body": "test body", "userId": 1}'
```
Note the status code each returns and confirm it matches the table above.

---

## Module 5 — Relational Database Basics

### 5.1 The Sample Schema

We'll use this schema throughout:

```sql
CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    customer_id INT REFERENCES customers(id),
    order_date DATE
);

CREATE TABLE order_items (
    id INT PRIMARY KEY,
    order_id INT REFERENCES orders(id),
    product_name VARCHAR(100),
    quantity INT,
    unit_price DECIMAL(10,2)
);
```

`customer_id` in `orders` and `order_id` in `order_items` are **foreign
keys** — they reference the primary key of another table, enforcing that you
can't have an order for a customer that doesn't exist.

### 5.2 Basic Queries

```sql
-- All orders for a specific customer
SELECT * FROM orders WHERE customer_id = 1;

-- Total spend per customer
SELECT c.name, SUM(oi.quantity * oi.unit_price) AS total_spent
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN order_items oi ON oi.order_id = o.id
GROUP BY c.name;

-- Customers who have NEVER placed an order
SELECT c.name
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.id IS NULL;
```

The last query is the classic use of `LEFT JOIN`: it keeps *every* row from
`customers`, even those with no matching row in `orders` (where the joined
columns come back `NULL`). An `INNER JOIN` there would have silently dropped
customers with zero orders — which is exactly what you *don't* want when the
whole point is finding customers with zero orders.

### 5.3 Writes

```sql
INSERT INTO customers (id, name, email) VALUES (1, 'Alice', 'alice@example.com');

UPDATE customers SET email = 'alice.new@example.com' WHERE id = 1;

DELETE FROM order_items WHERE order_id = 5;
```

**Always write the `WHERE` clause first, mentally, before the `UPDATE`/`DELETE`.**
An `UPDATE customers SET email = '...'` with no `WHERE` updates *every row in
the table*. Run a `SELECT` with the same `WHERE` clause first to confirm
which rows you're about to touch.

### 5.4 Indexes

Without an index, finding "all orders for customer 7" means scanning every
row in `orders` (a full table scan). An index on `customer_id`:

```sql
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

turns that into a fast lookup — but every `INSERT`/`UPDATE`/`DELETE` on
`orders` now also has to update the index, which costs write performance.
That's the trade-off: indexes speed up reads on the indexed column(s), at
the cost of slightly slower writes and extra storage. Index columns you
filter/join on frequently; don't index everything "just in case."

**Try it yourself:** Using SQLite, Postgres, or any DB you have locally,
create the schema above, insert a handful of rows, and run all the queries
in this module. Then run `EXPLAIN` (or `EXPLAIN ANALYZE` on Postgres) before
and after adding an index on `orders.customer_id`, and compare the plans.

---

## Module 6 — IDE Setup + Your First Spring Boot App

### 6.1 Generate the Project

Go to [start.spring.io](https://start.spring.io) (Spring Initializr), select:
- Maven, Java, latest stable Spring Boot version
- Dependency: **Spring Web**

Download and unzip it, open it in your IDE.

### 6.2 What You Got

```
src/main/java/com/example/demo/DemoApplication.java
src/main/resources/application.properties
pom.xml
```

`DemoApplication.java`:
```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

`@SpringBootApplication` is shorthand for three annotations combined:
`@Configuration` (this class can define beans), `@EnableAutoConfiguration`
(let Spring Boot guess sensible defaults based on what's on the classpath),
and `@ComponentScan` (find and register `@Component`/`@Service`/etc. classes
in this package and below).

### 6.3 Your First Endpoint

```java
package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World";
    }
}
```

Run `DemoApplication.main()`. Spring Boot starts an embedded Tomcat server,
default port `8080`. Visit `http://localhost:8080/hello` in a browser, or:

```bash
curl http://localhost:8080/hello
```

You should see `Hello, World`.

### 6.4 Changing the Port

In `application.properties`:
```
server.port=8081
```
Restart the app, and the same `curl http://localhost:8081/hello` now works
instead (the old port stops responding). This matters because multiple
services can't share a port on the same machine — one reason environment
configuration (coming in Phase 1) exists.

**Try it yourself:** Add a second endpoint, `GET /hello/{name}`, that returns
`"Hello, " + name` using `@PathVariable`. This is a small preview of Phase 2
— don't worry about understanding every annotation yet, just get it running.

```java
@GetMapping("/hello/{name}")
public String helloName(@PathVariable String name) {
    return "Hello, " + name;
}
```

---

## Phase 0 Completion Checklist

Work through this checklist honestly before moving to Phase 1:

- [ ] I can write a class with encapsulated fields, a constructor, and methods
- [ ] I can explain when to use inheritance vs an interface, and have used both
- [ ] I can use `List`, `Set`, and `Map` correctly, including grouping data into a `Map<K, List<V>>`
- [ ] I can create and throw both a checked and an unchecked exception, and explain the difference
- [ ] I can write a stream pipeline with `filter`, `map`, `sorted`, and `collect`
- [ ] I can explain why `equals()` and `hashCode()` must be overridden together
- [ ] I can build and package a Maven project from the command line, and read a dependency tree
- [ ] I can create a branch, commit, push, open a PR, and resolve a real merge conflict correctly
- [ ] I can explain idempotency and match HTTP status codes to the situations in this document
- [ ] I can write `SELECT`/`INSERT`/`UPDATE`/`DELETE` with joins, and explain what an index trades off
- [ ] I have a running Spring Boot app with at least two working endpoints

If any box is unchecked, go back to that module before starting Phase 1 —
everything after this assumes all of the above is second nature.

**Next:** [Phase 1 — Spring Boot Foundations](../phase-1-spring-boot-foundations/README.md)
