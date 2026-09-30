package com.fabflix.boot.movie;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieDetailController.class)
@Import(MovieApiExceptionHandler.class)
class MovieDetailControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieDetailService movieDetailService;

    @Test
    void returnsMovieUsingLegacyJsonContract() throws Exception {
        MovieDetail movie = new MovieDetail(
                "tt0078748",
                "Alien",
                1979,
                "Ridley Scott",
                "Horror, Sci-Fi",
                "8.5",
                List.of(new MovieDetailStar("nm0000244", "Sigourney Weaver"))
        );
        when(movieDetailService.getMovie("tt0078748")).thenReturn(movie);

        mockMvc.perform(get("/api/boot/single-movie").param("id", "tt0078748"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie_id").value("tt0078748"))
                .andExpect(jsonPath("$.movie_title").value("Alien"))
                .andExpect(jsonPath("$.movie_year").value(1979))
                .andExpect(jsonPath("$.movie_director").value("Ridley Scott"))
                .andExpect(jsonPath("$.movie_genres").value("Horror, Sci-Fi"))
                .andExpect(jsonPath("$.movie_rating").value("8.5"))
                .andExpect(jsonPath("$.movie_stars[0].star_id").value("nm0000244"))
                .andExpect(jsonPath("$.movie_stars[0].star_name").value("Sigourney Weaver"));

        verify(movieDetailService).getMovie("tt0078748");
    }

    @Test
    void returnsBadRequestForMissingId() throws Exception {
        when(movieDetailService.getMovie(null))
                .thenThrow(new IllegalArgumentException("Movie id is required."));

        mockMvc.perform(get("/api/boot/single-movie"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Movie id is required."));
    }

    @Test
    void preservesLegacyNotFoundContract() throws Exception {
        when(movieDetailService.getMovie("missing-id"))
                .thenThrow(new MovieNotFoundException("missing-id"));

        mockMvc.perform(get("/api/boot/single-movie").param("id", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Movie not found."));
    }

    @Test
    void returnsInternalServerErrorForUnexpectedFailure() throws Exception {
        when(movieDetailService.getMovie("tt0078748"))
                .thenThrow(new RuntimeException("database unavailable"));

        mockMvc.perform(get("/api/boot/single-movie").param("id", "tt0078748"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("database unavailable"));
    }
}
