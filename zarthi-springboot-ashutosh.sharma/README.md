# Spring Boot Fresher Bootcamp

A self-teaching, hands-on onboarding curriculum for freshers with **zero prior
Spring Boot experience**, built around the Spring Practice Service catalog.
You build **one application — a Library Service** — incrementally, phase by
phase, from a blank repo to a secured, event-driven, observable microservice.

This is designed to work as a **GitHub Classroom assignment**: each phase is
a milestone, the `project/` folder is the single codebase you build up over
the whole course, and each phase's README is a complete lesson you work
through on your own before checking in with a mentor.

## How this repo is organized

```
springboot-fresher-bootcamp/
├── README.md                          ← you are here
├── project/                           ← the ONE codebase you build, phase by phase
│   ├── pom.xml
│   └── src/main/java/com/example/library/
├── phase-0-prerequisites/README.md    ← Java, Git, HTTP, SQL, first Spring Boot app
├── phase-1-spring-boot-foundations/README.md
├── phase-2-rest-apis/README.md
├── phase-3-data-persistence/README.md
├── phase-4-external-integrations/README.md
├── phase-5-security/README.md
├── phase-6-business-logic-reliability/README.md
├── phase-7-event-driven/README.md
├── phase-8-governance-support/README.md
├── phase-9-lead-architect-awareness/README.md
└── .github/workflows/ci.yml           ← builds project/ on every push (GitHub Classroom friendly)
```

Each `phase-N.../README.md` is a **full lesson**: explanations, worked code
examples, and a "Your Task This Phase" section telling you exactly which
files under `project/` to create or edit. Each phase's `exercises/` folder
holds standalone practice exercises that don't belong in the main project
(e.g. isolated Java/SQL/Git drills) — do these *before* touching `project/`
for that phase.

## The domain: Library Service

You're building a backend for a library system: books, authors, borrowing,
reviews, notifications when a book is overdue. It's small enough to reason
about in a day, but touches every catalog service a Developer needs:

| Phase | What you add to the Library Service |
|---|---|
| 0 | Nothing yet — pure Java/Git/SQL/HTTP practice, plus your first throwaway Spring Boot app |
| 1 | Project skeleton, configuration profiles, packaging, Docker |
| 2 | REST API for Books (CRUD) |
| 3 | Persist Books/Authors (JPA) and Reviews (MongoDB) |
| 4 | ISBN lookup integration, overdue notifications, cover image storage, Redis caching |
| 5 | Librarian/patron authentication, rate limiting, secrets management |
| 6 | Borrow/return business logic, error handling, transactions, idempotent borrow requests, configurable borrow limits, a feature flag |
| 7 | `BookBorrowedEvent` / `BookReturnedEvent` via Kafka |
| 8 | PR workflow, fixing a slow search query, troubleshooting a seeded bug |
| 9 | Awareness of how this service fits a real platform (no code — design discussion) |

## How to use this as a GitHub Classroom assignment

1. **Instructor**: import this repo as a GitHub Classroom assignment template.
   Each student/pair gets their own copy.
2. **Student**: work through `phase-0-prerequisites/README.md` first, doing
   its `exercises/` before touching `project/`.
3. For each phase, create a branch: `git checkout -b phase-N-yourname`. Do
   the phase's work in `project/`, commit as you go (small, meaningful
   commits — see Phase 0, Module 3), then open a PR back to `main` for
   review. This *is* Phase 8's lesson in practice, so start the habit early.
4. `.github/workflows/ci.yml` runs `mvn -f project/pom.xml test` on every
   push — a red CI check means something in `project/` doesn't build or a
   test fails. Don't merge on red CI.
5. Move to the next phase only once the current phase's completion checklist
   (bottom of each README) is fully checked and your mentor has reviewed
   the PR.

## Prerequisites

Java 17+, Maven 3.9+, Docker, Git, an IDE (IntelliJ IDEA Community is fine).
Nothing else is assumed — Phase 0 teaches everything else from zero.

## For instructors

See [`GRADING.md`](./GRADING.md) for setting this up as a GitHub Classroom
template repo, and for the difference between the visible autograding tests
included here (`project/src/grading-test/java/.../grading/`, kept in a
separate source root so they never break compilation for students who
haven't reached that phase) and a truly-hidden-test setup for summative
grading.
