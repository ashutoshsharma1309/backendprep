package com.example.library.repository;

import com.example.library.model.Book;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {

    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(books.get(id));
    }

    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(nextId.getAndIncrement());
        }
        books.put(book.getId(), book);
        return book;
    }

    public boolean existsById(Long id) {
        return books.containsKey(id);
    }

    public void deleteById(Long id) {
        books.remove(id);
    }
}
