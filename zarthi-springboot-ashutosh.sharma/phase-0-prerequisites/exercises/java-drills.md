# Java Drills

Do these in order — each builds on the `Book`/`Media`/`Borrowable` classes
from Module 1 of the main lesson.

1. Create three `Book` objects and print them via `toString()`.
2. Create a `DVD` class extending `Media` and implementing `Borrowable`,
   with its own `describe()`. Build a `List<Media>` with both a `Book` and a
   `DVD`, and print each via `describe()` in a loop — this demonstrates
   polymorphism.
3. Build a `Map<String, List<Book>>` grouping books by author from a flat
   `List<Book>`.
4. Write `LibraryFullException` (unchecked) and `BookNotFoundException`
   (checked). Trigger both. Confirm the compiler forces you to handle one but
   not the other.
5. Using a `List<Book>`, use streams to: find the oldest book, count books
   published before 2010, and build a deduplicated, comma-separated string
   of authors.
6. Override only `equals()` (not `hashCode()`) on `Book`, add two "equal"
   books to a `HashSet<Book>`, and print its size — confirm it's `2`. Then
   add `hashCode()` and confirm the size becomes `1`.
