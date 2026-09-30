package com.example.library.controller;

import com.example.library.model.Book;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookRepository repository;

    @Test
    void getBook_returns200AndBody_whenBookExists() throws Exception {
        Book book = new Book(1L, "Effective Java", "Joshua Bloch", 2018);
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        mockMvc.perform(get("/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Effective Java"))
            .andExpect(jsonPath("$.author").value("Joshua Bloch"));
    }

    @Test
    void getBook_returns404_whenBookMissing() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/books/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("No book found with id: 999"));
    }

    @Test
    void createBook_returns201AndLocation_whenRequestIsValid() throws Exception {
        Book saved = new Book(7L, "Clean Code", "Robert Martin", 2008);
        when(repository.save(any(Book.class))).thenReturn(saved);

        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"title\":\"Clean Code\",\"author\":\"Robert Martin\",\"publishedYear\":2008}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/books/7"))
            .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createBook_returns400_whenTitleMissing() throws Exception {
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"author\":\"Someone\",\"publishedYear\":2020}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details[0]").value("title: title is required"));
    }

    @Test
    void createBook_returns400_whenPublishedYearIsImplausible() throws Exception {
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"title\":\"Ancient\",\"author\":\"Someone\",\"publishedYear\":900}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteBook_returns204_whenBookExists() throws Exception {
        when(repository.existsById(3L)).thenReturn(true);

        mockMvc.perform(delete("/books/3"))
            .andExpect(status().isNoContent());

        verify(repository).deleteById(3L);
    }

    @Test
    void deleteBook_returns404_whenBookMissing() throws Exception {
        when(repository.existsById(404L)).thenReturn(false);

        mockMvc.perform(delete("/books/404"))
            .andExpect(status().isNotFound());
    }
}
