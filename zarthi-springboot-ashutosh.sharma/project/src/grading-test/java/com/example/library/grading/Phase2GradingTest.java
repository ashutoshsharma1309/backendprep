package com.example.library.grading;

import com.example.library.model.Book;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GRADING TEST — Phase 2 (Building REST APIs).
 *
 * Tagged "grading" so instructors can run this set in isolation:
 *   mvn test -Dgroups=grading
 *
 * These check OBSERVABLE BEHAVIOR (status codes, response shape) rather
 * than internal class names, so students have implementation freedom as
 * long as the documented contract (see phase-2-rest-apis/README.md) is met.
 */
@Tag("grading")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Phase2GradingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository bookRepository;

    @Test
    void createBook_returns201WithLocationHeader() throws Exception {
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"title\":\"Test Book\",\"author\":\"Test Author\",\"publishedYear\":2020}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"));
    }

    @Test
    void createBook_withMissingTitle_returns400() throws Exception {
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content("{\"author\":\"Test Author\",\"publishedYear\":2020}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getBook_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/books/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteBook_thenGet_returns404() throws Exception {
        Book book = new Book();
        // Adjust to your actual constructor/setters if different —
        // this is the one place grading tests are coupled to Phase 3's
        // entity shape; update after Phase 3 lands.
        book.setTitle("To Delete");
        book.setAuthor("Someone");
        book.setPublishedYear(2020);
        Book saved = bookRepository.save(book);

        mockMvc.perform(delete("/books/" + saved.getId()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/books/" + saved.getId()))
            .andExpect(status().isNotFound());
    }
}
