package com.example.library.exception;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("No book found with id: " + id);
    }
}
