#!/bin/sh
set -e

DRILL_DIR=${1:-conflict-drill}

rm -rf "$DRILL_DIR"
mkdir -p "$DRILL_DIR"
cd "$DRILL_DIR"

git init -q -b main

cat > Search.java << 'EOF'
public class Search {
    public boolean matches(String author, String query) {
        return author.equals(query);
    }
}
EOF

cat > SearchTest.java << 'EOF'
public class SearchTest {
    static int failed = 0;

    static void check(String label, boolean actual, boolean expected) {
        if (actual != expected) {
            System.out.println("FAIL " + label + ": expected " + expected + ", got " + actual);
            failed++;
        } else {
            System.out.println("ok   " + label);
        }
    }

    public static void main(String[] args) {
        Search search = new Search();
        check("exact match", search.matches("Joshua Bloch", "Joshua Bloch"), true);
        check("different case", search.matches("Joshua Bloch", "joshua bloch"), true);
        check("no match", search.matches("Joshua Bloch", "Brian Goetz"), false);
        check("null author", search.matches(null, "Joshua Bloch"), false);
        if (failed > 0) {
            System.out.println(failed + " test(s) failed");
            System.exit(1);
        }
        System.out.println("all tests passed");
    }
}
EOF

git add Search.java SearchTest.java
git commit -q -m "Initial search implementation"

git checkout -q -b branch-a
cat > Search.java << 'EOF'
public class Search {
    public boolean matches(String author, String query) {
        return author.equalsIgnoreCase(query);
    }
}
EOF
git commit -q -am "Make search case-insensitive"

git checkout -q main
git checkout -q -b branch-b
cat > Search.java << 'EOF'
public class Search {
    public boolean matches(String author, String query) {
        if (author == null) {
            return false;
        }
        return author.equals(query);
    }
}
EOF
git commit -q -am "Guard against null author"

git checkout -q main
git merge -q branch-a
echo "merged branch-a cleanly"

if git merge branch-b; then
    echo "expected a conflict from branch-b but the merge succeeded"
    exit 1
fi

echo
echo "conflict markers left by git:"
grep -n '<<<<<<<\|=======\|>>>>>>>' Search.java
echo

cat > Search.java << 'EOF'
public class Search {
    public boolean matches(String author, String query) {
        if (author == null) {
            return false;
        }
        return author.equalsIgnoreCase(query);
    }
}
EOF

git add Search.java
git commit -q -m "Merge branch-b: keep null guard and case-insensitive match"

echo "resolved file:"
cat Search.java
echo

javac Search.java SearchTest.java
java SearchTest

echo
git log --oneline --graph --all
