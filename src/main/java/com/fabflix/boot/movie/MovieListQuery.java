package com.fabflix.boot.movie;

public class MovieListQuery {
    private final String title;
    private final String year;
    private final String director;
    private final String star;
    private final String genre;
    private final String startsWith;
    private final String fullText;
    private final int pageSize;
    private final int offset;
    private final String sort1;
    private final String dir1;
    private final String sort2;
    private final String dir2;

    public MovieListQuery(
            String title,
            String year,
            String director,
            String star,
            String genre,
            String startsWith,
            int pageSize,
            int offset,
            String sort1,
            String dir1,
            String sort2,
            String dir2) {
        this(title, year, director, star, genre, startsWith, pageSize, offset,
                sort1, dir1, sort2, dir2, null);
    }

    public MovieListQuery(
            String title, String year, String director, String star, String genre,
            String startsWith, int pageSize, int offset, String sort1, String dir1,
            String sort2, String dir2, String fullText) {
        // Store normalized request values that are safe for the repository to use.
        this.title = title;
        this.year = year;
        this.director = director;
        this.star = star;
        this.genre = genre;
        this.startsWith = startsWith;
        this.fullText = fullText;
        this.pageSize = pageSize;
        this.offset = offset;
        this.sort1 = sort1;
        this.dir1 = dir1;
        this.sort2 = sort2;
        this.dir2 = dir2;
    }

    public String getTitle() {
        // Return the normalized title filter.
        return title;
    }

    public String getYear() {
        // Return the normalized year filter.
        return year;
    }

    public String getDirector() {
        // Return the normalized director filter.
        return director;
    }

    public String getStar() {
        // Return the normalized star filter.
        return star;
    }

    public String getGenre() {
        // Return the normalized genre filter.
        return genre;
    }

    public String getStartsWith() {
        // Return the normalized startsWith filter.
        return startsWith;
    }

    public String getFullText() {
        return fullText;
    }

    public int getPageSize() {
        // Return the validated page size.
        return pageSize;
    }

    public int getOffset() {
        // Return the calculated SQL offset.
        return offset;
    }

    public String getSort1() {
        // Return the validated primary sort field.
        return sort1;
    }

    public String getDir1() {
        // Return the validated primary sort direction.
        return dir1;
    }

    public String getSort2() {
        // Return the validated secondary sort field.
        return sort2;
    }

    public String getDir2() {
        // Return the validated secondary sort direction.
        return dir2;
    }
}
