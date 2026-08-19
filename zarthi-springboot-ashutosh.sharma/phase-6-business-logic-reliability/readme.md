# Phase 6 — Core Business Logic & Reliability
### Prerequisite: Phase 5 complete. Branch: `git checkout -b phase-6-yourname`

**Catalog services:** Business Logic, Standardize Error Handling (both
Developer tier), Transaction Management, Idempotency, Configurable Business
Rules (Senior tier — pair on these), Feature Toggles.

This is the phase where the Library Service gets its actual core feature:
**borrowing and returning books.**

---

## Module 1 — Designing the Borrow Operation

### 1.1 What "Borrow a Book" Actually Requires

Resist the urge to just add a `borrowed: boolean` field and call it done.
A real borrow operation needs to:
1. Confirm the book exists and isn't already borrowed.
2. Confirm the patron hasn't hit their borrow limit (configurable — Module
   3 handles this).
3. Record who borrowed it and when, and compute a due date.
4. Do all of this atomically — no partial state if something fails midway.

### 1.2 The Domain Model

Add fields to `Book` (or create a `Loan` entity — a separate `Loan` is
cleaner and is what we'll build):

```java
package com.example.library.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    private String patronEmail;
    private LocalDate borrowedDate;
    private LocalDate dueDate;
    private LocalDate returnedDate; // null while still out

    public Loan() {}

    public Loan(Book book, String patronEmail, LocalDate borrowedDate, LocalDate dueDate) {
        this.book = book;
        this.patronEmail = patronEmail;
        this.borrowedDate = borrowedDate;
        this.dueDate = dueDate;
    }

    public boolean isActive() { return returnedDate == null; }

    // getters/setters omitted for brevity — add them for every field
    public Long getId() { return id; }
    public Book getBook() { return book; }
    public String getPatronEmail() { return patronEmail; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnedDate() { return returnedDate; }
    public void setReturnedDate(LocalDate d) { this.returnedDate = d; }
}
```

```java
package com.example.library.repository;

import com.example.library.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByPatronEmailAndReturnedDateIsNull(String patronEmail);
    Optional<Loan> findByBookIdAndReturnedDateIsNull(Long bookId);
}
```

---

## Module 2 — The Exception Hierarchy

```java
package com.example.library.exception;

public abstract class LibraryException extends RuntimeException {
    protected LibraryException(String message) { super(message); }
}
```

```java
package com.example.library.exception;

public class BookAlreadyBorrowedException extends LibraryException {
    public BookAlreadyBorrowedException(Long bookId) {
        super("Book " + bookId + " is already borrowed");
    }
}

public class BorrowLimitExceededException extends LibraryException {
    public BorrowLimitExceededException(String patronEmail, int limit) {
        super(patronEmail + " has reached the borrow limit of " + limit);
    }
}
```

Extend `GlobalExceptionHandler` from Phase 2 to map `LibraryException`
subtypes to `409 Conflict` (a business-rule conflict, not a client input
error — that distinction is why this isn't `400`):

```java
@ExceptionHandler(LibraryException.class)
public ResponseEntity<ErrorResponse> handleLibraryException(LibraryException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(ex.getMessage()));
}
```

---

## Module 3 — Configurable Business Rules

Remember `BorrowProperties` from Phase 1? Use it now instead of a hardcoded
number:

```java
package com.example.library.service;

import com.example.library.config.BorrowProperties;
import com.example.library.exception.*;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BorrowingService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final BorrowProperties borrowProperties;

    public BorrowingService(BookRepository bookRepository, LoanRepository loanRepository,
                             BorrowProperties borrowProperties) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.borrowProperties = borrowProperties;
    }

    @Transactional
    public Loan borrowBook(Long bookId, String patronEmail) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        if (loanRepository.findByBookIdAndReturnedDateIsNull(bookId).isPresent()) {
            throw new BookAlreadyBorrowedException(bookId);
        }

        int activeLoans = loanRepository.findByPatronEmailAndReturnedDateIsNull(patronEmail).size();
        if (activeLoans >= borrowProperties.getMaxBooksPerPatron()) {
            throw new BorrowLimitExceededException(patronEmail, borrowProperties.getMaxBooksPerPatron());
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(borrowProperties.getLoanPeriodDays());
        return loanRepository.save(new Loan(book, patronEmail, today, dueDate));
    }
}
```

**This is the point of Phase 1's `BorrowProperties`:** the borrow limit and
loan period are business decisions that change (a library might raise the
limit during a promotion), and now that only requires a config change, not a
code change and redeploy.

**Your task this phase:** add `POST /loans` (calls `borrowBook`) and confirm
via `curl` that: a second borrow of the same book returns `409`, and
exceeding `library.borrow.max-books-per-patron` also returns `409`.

---

## Module 4 — Idempotency

### 4.1 The Problem

A patron's phone loses signal right after they tap "borrow" — did the
request go through? Their app retries the same `POST /loans`. Without
protection, this creates **two loans for one book**, which then also
violates the "already borrowed" rule in a confusing way, or worse, silently
succeeds twice if you haven't enforced single-active-loan-per-book strictly.

### 4.2 Idempotency Keys

```java
package com.example.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    private Long resultLoanId;

    public IdempotencyRecord() {}
    public IdempotencyRecord(String key, Long resultLoanId) {
        this.idempotencyKey = key;
        this.resultLoanId = resultLoanId;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getResultLoanId() { return resultLoanId; }
}
```

```java
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {}
```

Update `BorrowingService`:

```java
@Transactional
public Loan borrowBookIdempotent(Long bookId, String patronEmail, String idempotencyKey) {
    var existing = idempotencyRepository.findById(idempotencyKey);
    if (existing.isPresent()) {
        Long loanId = existing.get().getResultLoanId();
        return loanRepository.findById(loanId).orElseThrow();
    }

    Loan loan = borrowBook(bookId, patronEmail); // reuses the logic from Module 3
    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, loan.getId()));
    return loan;
}
```

Update the controller to require an `Idempotency-Key` header:

```java
@PostMapping("/loans")
public Loan borrow(@RequestBody BorrowRequest request,
                    @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return borrowingService.borrowBookIdempotent(request.getBookId(), request.getPatronEmail(), idempotencyKey);
}
```

**Your task this phase:** call `POST /loans` twice with the **same**
`Idempotency-Key` header and same body, and confirm you get back the *same*
loan both times (check the `id` in the response), not a `409` and not a
second loan.

---

## Module 5 — Feature Toggles

```properties
library.features.recommendations-enabled=false
```

```java
@Component
@ConfigurationProperties(prefix = "library.features")
public class FeatureFlags {
    private boolean recommendationsEnabled;
    public boolean isRecommendationsEnabled() { return recommendationsEnabled; }
    public void setRecommendationsEnabled(boolean v) { this.recommendationsEnabled = v; }
}
```

```java
@GetMapping("/books/{id}/recommendations")
public List<Book> getRecommendations(@PathVariable Long id) {
    if (!featureFlags.isRecommendationsEnabled()) {
        return List.of();
    }
    // real recommendation logic here
    return List.of();
}
```

**Your task this phase:** toggle `library.features.recommendations-enabled`
between `true`/`false` via a config change (no code change, no rebuild) and
confirm the endpoint's behavior changes.

---

## Phase 6 Completion Checklist

- [ ] `Loan` entity and `BorrowingService.borrowBook` implemented with correct rule enforcement
- [ ] Exception hierarchy in place, mapped to `409` globally
- [ ] Borrow limit driven by `BorrowProperties`, provably configurable without code changes
- [ ] Idempotency-key handling proven to prevent duplicate loans on retry
- [ ] Feature flag implemented and toggled without a rebuild
- [ ] PR opened, mentor-reviewed (Transaction Management, Idempotency, and Rules are Senior-tier), CI green

**Next:** [Phase 7 — Event-Driven Development](../phase-7-event-driven/README.md)
