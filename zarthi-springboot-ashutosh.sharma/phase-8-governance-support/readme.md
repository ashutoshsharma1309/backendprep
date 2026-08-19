# Phase 8 — Developer Governance & Support
### Prerequisite: Phases 0–7 complete (run alongside earlier phases once Git is comfortable)

**Catalog services:** Manage Code Integration (Senior tier), Resolve Merge
Conflicts (Developer tier — you should be fully independent here), SQL
Optimization (Senior tier), Troubleshoot Application Issues (Senior tier).

This phase has no single new feature to build — it's three exercises against
the codebase you already have, plus the PR habits you should already be
practicing from every earlier phase.

---

## Module 1 — Code Integration, For Real

By now you've opened seven PRs (one per phase). Before this phase, go back
and self-review your own Phase 6 or 7 PR as if you were reviewing a
teammate's:

- Does the PR description explain *why*, not just *what*?
- Is there a test for the new behavior, or just manual `curl` verification?
- Would a reviewer unfamiliar with your last three commits understand this
  diff on its own?

**Your task this phase:** pair with another trainee (or your mentor) and do
a real cross-review of each other's Phase 6 or 7 PR. Leave at least one
comment that isn't a nitpick — something about correctness, a missed edge
case, or a test gap. Respond to at least one comment on your own PR with an
actual code change, not just a reply.

---

## Module 2 — A Realistic Merge Conflict

Phase 0 gave you a small, artificial conflict. This one is deliberately
messier, on real application code.

**Setup:** create two branches off `main`, both editing
`BorrowingService.borrowBook`:

- **Branch A:** add a log statement right before the loan is saved:
  `log.info("Borrowing book {} for {}", bookId, patronEmail);`
- **Branch B:** change the due-date calculation to skip weekends (a small
  real behavior change to the same method).

Merge branch A first (clean). Merge branch B — this conflicts, because both
touched the body of `borrowBook`.

**Resolve it correctly:** the final method must have **both** the log
statement and the weekend-skipping due-date logic. After resolving, run
`mvn test` — a clean-looking resolution that still leaves your Phase 6
tests failing is not actually resolved.

```java
// Weekend-skipping logic, for Branch B, if you need it:
LocalDate dueDate = today.plusDays(borrowProperties.getLoanPeriodDays());
while (dueDate.getDayOfWeek() == DayOfWeek.SATURDAY || dueDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
    dueDate = dueDate.plusDays(1);
}
```

---

## Module 3 — SQL Optimization

### 3.1 Seed Enough Data to Matter

Small dev datasets never show real performance problems. Write a quick
seeding script (a `CommandLineRunner` bean, disabled by default, or a
one-off test) that inserts a few thousand `Loan` records across a few
hundred books and patrons.

```java
@Bean
@Profile("seed")
public CommandLineRunner seedLoans(LoanRepository loanRepository, BookRepository bookRepository) {
    return args -> {
        // insert enough Book and Loan rows (1000+) to make a full scan visible
    };
}
```

### 3.2 Find and Fix a Slow Query

A derived query like this looks harmless:

```java
List<Loan> findByPatronEmail(String patronEmail);
```

Without an index on `patron_email`, this is a full table scan once the
`loans` table has thousands of rows. Check the plan:

```sql
EXPLAIN ANALYZE SELECT * FROM loans WHERE patron_email = 'someone@example.com';
```

Look for `Seq Scan` (Postgres) instead of an index-based scan. Fix it:

```java
@Entity
@Table(name = "loans", indexes = {
    @Index(name = "idx_loans_patron_email", columnList = "patronEmail")
})
public class Loan { /* ... */ }
```

Re-run `EXPLAIN ANALYZE` and confirm the plan now uses the index.

**Your task this phase:** do this for real against your seeded data —
capture the "before" plan, add the index, capture the "after" plan, and
include both in your PR description as evidence, not just a claim that it's
faster.

---

## Module 4 — Troubleshooting a Seeded Bug

**Your mentor will seed a bug into your branch** (or, working solo, ask a
peer to introduce one deliberately — e.g. swap `<=` for `<` in the borrow
limit check, or remove the `@CacheEvict` from Phase 4's `updatePopularBook`).

**Diagnose it methodically, in this order:**
1. Read the bug report / reproduction steps as if you didn't write the code.
2. Check logs first — what does the application actually say happened?
3. Form a hypothesis *before* opening the source file.
4. Confirm the hypothesis by reproducing it locally.
5. Only then look at the code to find the root cause.
6. Fix it, and write a test that would have caught it.

Write a short root-cause summary:
- **What happened:**
- **Root cause:**
- **Fix:**
- **How we'll prevent this class of bug in the future** (usually: "a test now covers this")

---

## Phase 8 Completion Checklist

- [ ] Did a real cross-review of a peer's PR with substantive feedback
- [ ] Resolved a realistic conflict touching real business logic, verified with tests afterward
- [ ] Seeded realistic data volume and captured a genuine slow-query execution plan
- [ ] Added an index and captured the improved plan as evidence
- [ ] Diagnosed a seeded bug methodically (logs → hypothesis → repro → fix) and wrote a root-cause summary
- [ ] Added a regression test for the bug found

**Next:** [Phase 9 — Lead & Architect Awareness](../phase-9-lead-architect-awareness/README.md)
