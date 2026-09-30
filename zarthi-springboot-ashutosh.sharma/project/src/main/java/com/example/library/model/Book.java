package com.example.library.model;

public class Book {

    private Long id;
    private String title;
    private String author;
    private int publishedYear;

    public Book() {
    }

    public Book(Long id, String title, String author, int publishedYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(int publishedYear) {
        this.publishedYear = publishedYear;
    }
}
