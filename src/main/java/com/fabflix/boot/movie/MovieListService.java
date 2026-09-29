package com.fabflix.boot.movie;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MovieListService {
    private final MovieListQueryFactory movieListQueryFactory;
    private final MovieListRepository movieListRepository;

    public MovieListService(
            MovieListQueryFactory movieListQueryFactory,
            MovieListRepository movieListRepository) {
        this.movieListQueryFactory = movieListQueryFactory;
        this.movieListRepository = movieListRepository;
    }

    public List<MovieSummary> listMovies(MovieListRequest request) {
        MovieListQuery query = movieListQueryFactory.fromRequest(request);
        return movieListRepository.findMovies(query);
    }
}
