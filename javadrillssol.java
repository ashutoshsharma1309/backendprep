import java.util.*;
import java.util.stream.Collectors;

public class javadrillssol {

    static abstract class Media {
        protected final String title;

        Media(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }

        abstract String describe();
    }

    interface Borrowable {
        void borrow(String borrowerName);
        void returnItem();
    }

    static class Book extends Media implements Borrowable {
        private final String author;
        private final int publishedYear;
        private boolean borrowed = false;

        Book(String title, String author, int publishedYear) {
            super(title);
            this.author = author;
            this.publishedYear = publishedYear;
        }

        String getAuthor() {
            return author;
        }

        int getPublishedYear() {
            return publishedYear;
        }

        @Override
        String describe() {
            return "Book: " + title + " by " + author;
        }

        @Override
        public void borrow(String borrowerName) {
            if (borrowed) {
                throw new IllegalStateException("Already borrowed: " + title);
            }
            borrowed = true;
            System.out.println("  " + title + " borrowed by " + borrowerName);
        }

        @Override
        public void returnItem() {
            borrowed = false;
        }

        @Override
        public String toString() {
            return title + " by " + author + " (" + publishedYear + ")";
        }

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
    }

    static class DVD extends Media implements Borrowable {
        private final String director;
        private final int runtimeMinutes;
        private boolean borrowed = false;

        DVD(String title, String director, int runtimeMinutes) {
            super(title);
            this.director = director;
            this.runtimeMinutes = runtimeMinutes;
        }

        @Override
        String describe() {
            return "DVD: " + title + " directed by " + director + ", " + runtimeMinutes + " min";
        }

        @Override
        public void borrow(String borrowerName) {
            if (borrowed) {
                throw new IllegalStateException("Already borrowed: " + title);
            }
            borrowed = true;
            System.out.println("  " + title + " borrowed by " + borrowerName);
        }

        @Override
        public void returnItem() {
            borrowed = false;
        }
    }

    static class LibraryFullException extends RuntimeException {
        LibraryFullException(int capacity) {
            super("Library is full, capacity is " + capacity);
        }
    }

    static class BookNotFoundException extends Exception {
        BookNotFoundException(String title) {
            super("No book found with title: " + title);
        }
    }

    static class Library {
        private final int capacity;
        private final Map<String, Book> booksByTitle = new HashMap<>();

        Library(int capacity) {
            this.capacity = capacity;
        }

        void add(Book book) {
            if (booksByTitle.size() >= capacity) {
                throw new LibraryFullException(capacity);
            }
            booksByTitle.put(book.getTitle(), book);
        }

        Book find(String title) throws BookNotFoundException {
            Book book = booksByTitle.get(title);
            if (book == null) {
                throw new BookNotFoundException(title);
            }
            return book;
        }
    }

    static class BookEqualsOnly {
        private final String title;
        private final String author;

        BookEqualsOnly(String title, String author) {
            this.title = title;
            this.author = author;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BookEqualsOnly)) return false;
            BookEqualsOnly other = (BookEqualsOnly) o;
            return title.equals(other.title) && author.equals(other.author);
        }
    }

    static void drill1() {
        System.out.println("=== Drill 1: toString() ===");
        Book b1 = new Book("Effective Java", "Joshua Bloch", 2018);
        Book b2 = new Book("Clean Code", "Robert Martin", 2008);
        Book b3 = new Book("Java Concurrency in Practice", "Brian Goetz", 2006);
        System.out.println(b1);
        System.out.println(b2);
        System.out.println(b3);
        System.out.println();
    }

    static void drill2() {
        System.out.println("=== Drill 2: polymorphism ===");
        List<Media> items = new ArrayList<>();
        items.add(new Book("Effective Java", "Joshua Bloch", 2018));
        items.add(new DVD("Inception", "Christopher Nolan", 148));

        for (Media item : items) {
            System.out.println(item.describe());
        }

        for (Media item : items) {
            if (item instanceof Borrowable) {
                ((Borrowable) item).borrow("Ashutosh");
            }
        }
        System.out.println();
    }

    static void drill3() {
        System.out.println("=== Drill 3: grouping by author ===");
        List<Book> books = sampleBooks();

        Map<String, List<Book>> byAuthor = new HashMap<>();
        for (Book book : books) {
            byAuthor.computeIfAbsent(book.getAuthor(), author -> new ArrayList<>()).add(book);
        }

        for (Map.Entry<String, List<Book>> entry : byAuthor.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue().size() + " book(s)");
            for (Book book : entry.getValue()) {
                System.out.println("    " + book.getTitle());
            }
        }

        Map<String, List<Book>> viaStream = books.stream()
            .collect(Collectors.groupingBy(Book::getAuthor));
        System.out.println("same keys from groupingBy: " + viaStream.keySet().equals(byAuthor.keySet()));
        System.out.println();
    }

    static void drill4() {
        System.out.println("=== Drill 4: checked vs unchecked ===");
        Library library = new Library(2);
        library.add(new Book("Effective Java", "Joshua Bloch", 2018));
        library.add(new Book("Clean Code", "Robert Martin", 2008));

        try {
            library.add(new Book("Refactoring", "Martin Fowler", 1999));
        } catch (LibraryFullException e) {
            System.out.println("unchecked caught: " + e.getMessage());
        }

        try {
            library.find("Nonexistent Title");
        } catch (BookNotFoundException e) {
            System.out.println("checked caught:   " + e.getMessage());
        }
        System.out.println();
    }

    static void drill5() {
        System.out.println("=== Drill 5: streams ===");
        List<Book> books = sampleBooks();

        Optional<Book> oldest = books.stream()
            .min(Comparator.comparingInt(Book::getPublishedYear));
        System.out.println("oldest book: " + oldest.map(Book::toString).orElse("none"));

        long before2010 = books.stream()
            .filter(b -> b.getPublishedYear() < 2010)
            .count();
        System.out.println("published before 2010: " + before2010);

        String authors = books.stream()
            .map(Book::getAuthor)
            .collect(Collectors.toSet())
            .stream()
            .sorted()
            .collect(Collectors.joining(", "));
        System.out.println("distinct authors: " + authors);
        System.out.println();
    }

    static void drill6() {
        System.out.println("=== Drill 6: equals() and hashCode() ===");

        BookEqualsOnly a1 = new BookEqualsOnly("Effective Java", "Joshua Bloch");
        BookEqualsOnly a2 = new BookEqualsOnly("Effective Java", "Joshua Bloch");
        Set<BookEqualsOnly> broken = new HashSet<>();
        broken.add(a1);
        broken.add(a2);
        System.out.println("a1.equals(a2) = " + a1.equals(a2));
        System.out.println("HashSet size with equals() only: " + broken.size());
        System.out.println("hashes differ: " + (a1.hashCode() != a2.hashCode()));

        Book b1 = new Book("Effective Java", "Joshua Bloch", 2018);
        Book b2 = new Book("Effective Java", "Joshua Bloch", 2018);
        Set<Book> fixed = new HashSet<>();
        fixed.add(b1);
        fixed.add(b2);
        System.out.println("b1.equals(b2) = " + b1.equals(b2));
        System.out.println("HashSet size with equals() and hashCode(): " + fixed.size());
        System.out.println("hashes match: " + (b1.hashCode() == b2.hashCode()));
        System.out.println();
    }

    private static List<Book> sampleBooks() {
        return new ArrayList<>(List.of(
            new Book("Effective Java", "Joshua Bloch", 2018),
            new Book("Java Puzzlers", "Joshua Bloch", 2005),
            new Book("Clean Code", "Robert Martin", 2008),
            new Book("Java Concurrency in Practice", "Brian Goetz", 2006),
            new Book("Refactoring", "Martin Fowler", 1999)
        ));
    }

    public static void main(String[] args) {
        drill1();
        drill2();
        drill3();
        drill4();
        drill5();
        drill6();
    }
}
