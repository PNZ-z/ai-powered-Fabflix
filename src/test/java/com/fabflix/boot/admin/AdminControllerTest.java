package com.fabflix.boot.admin;

import static org.mockito.Mockito.*;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {
    @Test void returnsMetadataUsingLegacyFieldNames() throws Exception {
        AdminService service = mock(AdminService.class);
        when(service.metadata()).thenReturn(Collections.singletonList(new MetadataTable("movies",
                Collections.singletonList(new MetadataColumn("id", "VARCHAR(10)")))));
        MockMvcBuilders.standaloneSetup(new AdminController(service)).build()
                .perform(get("/api/boot/dashboard").param("action", "metadata"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.tables[0].table_name").value("movies"))
                .andExpect(jsonPath("$.tables[0].columns[0].column_name").value("id"));
    }

    @Test void delegatesAddStarFormParameters() throws Exception {
        AdminService service = mock(AdminService.class);
        when(service.addStar("Jane", "1980")).thenReturn(AdminResponse.success("created"));
        MockMvcBuilders.standaloneSetup(new AdminController(service)).build()
                .perform(post("/api/boot/dashboard").param("action", "add-star")
                        .param("star_name", "Jane").param("birth_year", "1980"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
    }

    @Test void delegatesAddMovieFormParameters() throws Exception {
        AdminService service = mock(AdminService.class);
        when(service.addMovie("T", "2000", "D", "S", "1980", "G"))
                .thenReturn(AdminResponse.success("created"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(service)).build();
        mvc.perform(post("/api/boot/dashboard").param("action", "add-movie")
                        .param("movie_title", "T").param("movie_year", "2000")
                        .param("movie_director", "D").param("movie_star_name", "S")
                        .param("movie_star_birth_year", "1980").param("movie_genre_name", "G"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
    }
}
