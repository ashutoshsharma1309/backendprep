# Phase 9 — Awareness Pass: Lead & Architect Responsibilities
### Prerequisite: Phases 0–8 complete

**This phase is deliberately not hands-on.** You've now built every piece of
the Library Service a Developer owns. This phase is about seeing the design
decisions that were made *for* you before you started coding each phase —
and understanding why those decisions sit above the Developer tier.

There is no `project/` code to write this phase. Work through each session
as a discussion with a mentor or peer group, using your own completed
codebase as the concrete example.

---

## Session 1 — Running Services in an Environment

### Environment Configuration, at Platform Scale

In Phase 1, you split config into `application-dev.properties` and
`application-prod.properties` for **one service**. Now imagine five services
(Library Service, a Notification Service, a Catalog Service, a Payments
Service, a Reporting Service), each needing consistent dev/staging/prod
configuration, shared secrets management (Phase 5, but now for five
services), and consistent deployment behavior. Someone has to decide: is
config centralized (a config server all services pull from) or per-service?
Is that decision made once, or does each team improvise independently?

**Discuss with your group:** Look at your `application-dev.properties`
/ `application-prod.properties` from Phase 1. If four other services each
made *different* choices about what's a profile vs an environment variable
vs a hardcoded default, what problems would that create for whoever operates
all five services together?

### Service Discovery

Right now, if the Library Service needed to call a hypothetical
"Notification Service" directly, you'd hardcode its URL:
```java
webClient.get().uri("http://notification-service.internal:8080/send")
```
This breaks the moment that service moves, scales to multiple instances, or
changes ports. **Service discovery** (Eureka, Consul, or Kubernetes-native
DNS-based discovery) lets a service ask "where is notification-service
right now?" instead of having that answer hardcoded.

**Discuss:** what would happen to your `IsbnLookupClient` from Phase 4 if
Open Library's URL changed tomorrow, versus if it were a discoverable
internal service instead of an external one you don't control?

### API Gateway

Your Library Service currently exposes `/books`, `/loans`, `/auth/login`
directly. In a real platform, external clients typically don't hit services
directly — they go through an **API Gateway**, which centralizes routing,
enforces the rate limiting and auth checks you built per-service in Phase 5
at a single edge point, and can apply policies (throttling, logging,
request shaping) consistently across every service behind it.

**Discuss:** what's the difference between the rate limiting you built
inside the Library Service in Phase 5, and rate limiting enforced at a
gateway in front of five services? What are the trade-offs of each?

### Observability at the Platform Level

You have `System.out.println`/logging inside your service (Phase 4, Phase
8). That's **one service's** logs. Real observability means: can you trace
a single patron's borrow request as it flows through Library Service →
Notification Service → (eventually) an email provider, correlating logs,
metrics, and traces across all of them? That's a platform-level design
concern — typically: structured logging with a shared correlation/trace ID,
centralized log aggregation, and distributed tracing (e.g. OpenTelemetry).

**Discuss:** in Phase 8's troubleshooting exercise, you diagnosed a bug
using logs from **one service**. What would you need if the bug actually
lived in the interaction *between* two services?

---

## Session 2 — Design Before Implementation

### Data Model Design

In Phase 3, you were handed a specific schema shape:
`Book` → `Author` (relational), `Review` (document). Imagine instead you'd
been told "figure out how to store books, authors, and reviews" with no
guidance. The choice to normalize `Book`/`Author` but embed `Review` data
wasn't arbitrary — it reflects how each is queried and how often it changes.
Getting this wrong *after* real data exists is expensive: a schema
migration on a live `books` table with millions of rows is a very different
problem than getting the model right on day one.

**Discuss:** look back at your Phase 3 work. What would have to happen —
technically and organizationally — if you decided *today* that `Review`
should have been relational instead of a MongoDB document?

### API Contract Design

Phase 2 handed you a specific shape for `BookRequest` and the REST
endpoints. In a real project, that shape is usually **designed and reviewed
before implementation** — contract-first — precisely so that whoever
consumes this API (a frontend team, another service) can build against a
stable contract without waiting for your implementation, and so that a
breaking shape change gets caught in review, not after three other teams
already integrated against it.

**Discuss:** if you'd designed `BookRequest` yourself, freely, before Phase
2, would you have made the same choices? What happens to every consumer of
`/books` if that shape changes now, after Phase 7's event consumers and any
hypothetical frontend already depend on it?

### External API Contract Design

Your `IsbnLookupClient` (Phase 4) depends on Open Library's contract — one
you don't control. Architect-level design here means anticipating: what
happens if that API changes its response shape? Deprecates the endpoint?
Rate-limits you unexpectedly? This requires more defensive design than an
internal contract you control.

### API Security Design

Phase 5 had you implement authentication and rate limiting **inside** the
Library Service. A platform-level security design decides things like: is
authentication centralized (one identity provider all services trust) or
does each service roll its own (as you did)? What's the platform's stance
on token expiry, refresh tokens, service-to-service auth (not just
user-to-service)? These are policy decisions made once, applied everywhere
— not decisions each Developer should make independently per service.

### Business Logic Design

Phase 6's borrow rules (max books per patron, loan period, the exception
hierarchy) were specified for you. In practice, an Architect or Lead often
decomposes a larger business process (e.g. "the full lifecycle of a book,
from acquisition through retirement") into service boundaries — deciding
*where* the Library Service's responsibility ends and another service's
begins — before any Developer starts implementing a piece of it.

### Configuration Architecture

Ties back to Session 1: deciding the overall strategy (config server vs.
per-service files, what's centralized vs. local) is the Architect-level
decision; Phase 1's `application-dev.properties` was you implementing
*within* that already-decided strategy.

### Event Schema Design

Phase 7's `BookBorrowedEvent` shape was specified for you — including the
decision to embed `bookTitle` rather than just `bookId`. Once other
services (your Notification Service, maybe a Reporting Service) depend on
that event shape, changing it becomes expensive in exactly the way changing
`BookRequest`'s shape would be — except now it might break services you
don't own and can't coordinate with as easily as an internal REST contract.
This is why event schemas usually get **versioned** deliberately
(`BookBorrowedEventV2`) rather than mutated in place.

**Discuss:** if you needed to add a `dueDate` field to `BookBorrowedEvent`
today, would you change the existing event, or add a new version? What
breaks if a consumer built against the old shape suddenly receives fields
it didn't expect, or is missing a field it depended on?

---

## Closing Exercise

Pick **one** decision from any earlier phase that was handed to you as a
given (a schema, an endpoint shape, an event payload, a config strategy).
Write a short paragraph: who do you think decided this, why, and what would
have gone wrong if a Developer had made a different choice unilaterally
mid-implementation, without review?

---

## Phase 9 Completion Checklist

- [ ] Can explain what problem service discovery and an API gateway each solve, using your own services as the example
- [ ] Can explain the difference between one service's logs and platform-level observability
- [ ] Can articulate why data models, API contracts, and event schemas are designed before implementation
- [ ] Completed the closing exercise, identifying a design decision that was made for you and why

**This closes the fresher onboarding curriculum.** You're ready for
independent Developer-tier ownership on real Library Service work, with
Senior-tier services (flagged throughout Phases 4–8) building progressively
under review.
