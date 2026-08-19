# Instructor Guide: Setting Up GitHub Classroom

## 1. Create the template repository

1. In your personal account or org, create a **new private repository**
   named `springboot-training-template`.
2. Push this entire folder's contents to it:
   ```bash
   cd springboot-fresher-bootcamp
   git init
   git add .
   git commit -m "Initial fresher bootcamp template"
   git branch -M main
   git remote add origin https://github.com/<your-org>/springboot-training-template.git
   git push -u origin main
   ```
3. In the repo's **Settings → General**, check **"Template repository."**
   This is what lets GitHub Classroom clone it per-student instead of
   forking.
4. In **GitHub Classroom** (classroom.github.com), create a new assignment,
   point it at `springboot-training-template`, and choose **individual or
   group repos** depending on how you want trainees to work.

This repo already contains everything the standard tutorial pattern asks
for:
- Spring Boot source under `project/src/main/java` (grows phase by phase)
- Student tests under `project/src/test/java`, always compiled and run
- Grading tests under `project/src/grading-test/java` — **deliberately a
  separate source root**, only compiled/run via `mvn verify -Pgrading` (see
  next section for why)
- A `README.md` per phase explaining that phase's goals, with full lesson
  content and worked code, not just pointers

## 2. Why grading tests live in a separate source root

`project/src/grading-test/java/.../grading/*.java` references classes
students only create in later phases (`Book` in Phase 2/3, the `/loans`
endpoint in Phase 6, etc.). If those tests sat in the default
`src/test/java`, **the whole module would fail to compile** for any student
who hasn't reached that phase yet — not a failing test, a broken build,
which also blocks their own passing tests from running. The `grading` Maven
profile (see `project/pom.xml`) adds `src/grading-test/java` as a test
source only when explicitly invoked with `-Pgrading` — students' everyday
`mvn test` / `mvn clean verify` never touches it and stays green based on
their own work.

## 3. Important: "hidden" grading tests aren't actually hidden by default

Any file committed to `springboot-training-template` — including everything
under `project/src/grading-test/java/` — is cloned into every student's repo
and is fully readable by them. GitHub Classroom's autograding
(`.github/workflows/classroom-autograding.yml` +
`.github/classroom/autograding.json`) runs these tests and reports a score,
but "runs and scores" is not the same as "hidden." Decide which you need:

### Option A — Visible grading tests (what's active in this repo)

The `src/grading-test/java` tree and `autograding.json` in this repo. Good for:
- Formative, self-check-style grading (students can see exactly what's
  tested, which helps them debug their own work — this fits a *training*
  context well, versus a high-stakes exam).
- Low setup overhead — nothing else to configure.

**Trade-off:** a student can read the assertions and could special-case
their code to satisfy the literal test rather than the real requirement.
For a fresher bootcamp this is usually an acceptable, even useful, trade-off
— but know that's what you're choosing.

### Option B — Truly hidden grading tests

For summative/high-stakes grading where students genuinely shouldn't see
the assertions:

1. Create a **second, separate private repository** —
   e.g. `springboot-training-hidden-tests` — containing only test classes,
   mirroring the package structure of
   `project/src/grading-test/java/com/example/library/grading/`.
2. Generate a fine-grained **Personal Access Token** scoped to read-only
   access on that one repo. Store it as an Actions secret named
   `HIDDEN_TESTS_PAT` in `springboot-training-template` (Settings → Secrets
   and variables → Actions) — Classroom copies repo secrets into each
   student repo it creates, so this propagates automatically.
3. In `.github/workflows/classroom-autograding.yml`, uncomment **Option B**
   and delete `project/src/grading-test/` from this template before
   publishing it, so students never see the real assertions — only that a
   step named "Run hidden grading tests" ran and reported a score.
4. Update assertions in the hidden repo as your solution key evolves,
   without ever touching the student-facing template.

**Trade-off:** more setup, and it depends on your CI runner being able to
reach GitHub with that token (fine on GitHub-hosted runners; check firewall
rules if you use self-hosted runners).

Most bootcamps mix the two: **visible** grading tests for early phases
(0–4, where the goal is learning, not gatekeeping) and **hidden** tests for
later phases (6–8, where you want to confirm independent competency before
sign-off).

## 3. Keeping `project/pom.xml` buildable at every phase

Notice `project/pom.xml` has several dependencies commented out (JPA,
MongoDB, Redis, Security, Kafka) — this is deliberate. The template must
build (`mvn clean verify`) from commit one, before any phase work has
happened, or `.github/workflows/ci.yml` is red for every student
immediately, which is confusing rather than motivating. Each phase's README
tells students exactly which block to uncomment when they reach it.

## 4. Branch-per-phase workflow (recommended)

Ask students to open a PR per phase (`phase-N-<name>` → `main`), per the
instructions in the root `README.md`. This gives you:
- A natural checkpoint per phase to review before they continue
- Real practice at the Phase 8 (Code Integration) skills, reinforced
  starting from Phase 1 instead of introduced cold
- A clean audit trail of progress per student, visible in Classroom's
  assignment dashboard (which shows commits/PRs, not just a final snapshot)

## 5. What NOT to change per student cohort

Keep `project/src/main/java/com/example/library/LibraryServiceApplication.java`
and the base package name stable across cohorts — the grading tests
(visible or hidden) reference `com.example.library.*`. If you rename the
package, update both the template and the hidden-tests repo together.
