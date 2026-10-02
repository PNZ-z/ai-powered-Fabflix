package com.fabflix.boot.main;

import com.fabflix.boot.auth.AuthenticatedCustomer;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MainController.class)
class MainControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenreService genreService;

    @Test
    void returnsGenresUsingLegacyJsonContract() throws Exception {
        when(genreService.listGenres()).thenReturn(List.of(new Genre("Action"), new Genre("Comedy")));

        mockMvc.perform(get("/api/boot/main")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[1].name").value("Comedy"));
    }

    @Test
    void returnsDatabaseFailureUsingLegacyErrorContract() throws Exception {
        when(genreService.listGenres()).thenThrow(new RuntimeException("database unavailable"));

        mockMvc.perform(get("/api/boot/main")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("database unavailable"));
    }
}
