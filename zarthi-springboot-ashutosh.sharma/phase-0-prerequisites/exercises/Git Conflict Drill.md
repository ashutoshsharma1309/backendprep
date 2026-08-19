# Solo Merge Conflict Drill

You don't need a partner to practice real conflicts. Do this in a scratch repo:

```bash
mkdir conflict-drill && cd conflict-drill && git init
cat > Search.java << 'EOF'
public class Search {
    public boolean matches(String author, String query) {
        return author.equals(query);
    }
}
EOF
git add . && git commit -m "Initial search implementation"

git checkout -b branch-a
# Edit Search.java: make matches() case-insensitive (use equalsIgnoreCase)
git commit -am "Make search case-insensitive"

git checkout main
git checkout -b branch-b
# Edit Search.java: add a null-check that returns false if author is null
git commit -am "Guard against null author"

git checkout main
git merge branch-a      # succeeds cleanly
git merge branch-b      # CONFLICTS — same lines touched by both branches
```

Resolve the conflict by hand so the final version has **both** the
null-check *and* case-insensitivity — neither branch's intent should be
lost. Then run your build/tests to confirm the resolved file actually
compiles and behaves correctly, not just that Git stopped complaining.