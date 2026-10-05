package com.fabflix.boot.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boot/dashboard")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping(params = "action=metadata")
    public MetadataResponse metadata() throws Exception {
        logger.info("Employee requested database metadata");
        return new MetadataResponse(adminService.metadata());
    }

    @PostMapping(params = "action=add-star")
    public AdminResponse addStar(@RequestParam("star_name") String name,
                                 @RequestParam(value = "birth_year", required = false) String birthYear) {
        AdminResponse response = adminService.addStar(name, birthYear);
        logger.info("Employee add-star request completed with status={}", response.getStatus());
        return response;
    }

    @PostMapping(params = "action=add-movie")
    public AdminResponse addMovie(@RequestParam(value = "movie_title", required = false) String title,
                                  @RequestParam(value = "movie_year", required = false) String year,
                                  @RequestParam(value = "movie_director", required = false) String director,
                                  @RequestParam(value = "movie_star_name", required = false) String starName,
                                  @RequestParam(value = "movie_star_birth_year", required = false) String starBirthYear,
                                  @RequestParam(value = "movie_genre_name", required = false) String genreName) {
        AdminResponse response = adminService.addMovie(title, year, director, starName, starBirthYear, genreName);
        logger.info("Employee add-movie request completed with status={}", response.getStatus());
        return response;
    }
}
