package com.fabflix.boot.movie;

public class MovieListRequest {
    private String title;
    private String year;
    private String director;
    private String star;
    private String genre;
    private String startsWith;
    private String pageNumber;
    private String pageSize;
    private String sort1;
    private String dir1;
    private String sort2;
    private String dir2;

    public MovieListRequest() {

    }

    public MovieListRequest(
            String title,
            String year,
            String director,
            String star,
            String genre,
            String startsWith,
            String pageNumber,
            String pageSize,
            String sort1,
            String dir1,
            String sort2,
            String dir2) {
        setTitle(title);
        setYear(year);
        setDirector(director);
        setStar(star);
        setGenre(genre);
        setStartsWith(startsWith);
        setPageNumber(pageNumber);
        setPageSize(pageSize);
        setSort1(sort1);
        setDir1(dir1);
        setSort2(sort2);
        setDir2(dir2);
    }

    public String getTitle() {
        // Return the raw title search parameter.
        return title;
    }

    public void setTitle(String title) {
        // Store the raw title search parameter.
        this.title = title;
    }

    public String getYear() {
        // Return the raw year search parameter.
        return year;
    }

    public void setYear(String year) {
        // Store the raw year search parameter.
        this.year = year;
    }

    public String getDirector() {
        // Return the raw director search parameter.
        return director;
    }

    public void setDirector(String director) {
        // Store the raw director search parameter.
        this.director = director;
    }

    public String getStar() {
        // Return the raw star search parameter.
        return star;
    }

    public void setStar(String star) {
        // Store the raw star search parameter.
        this.star = star;
    }

    public String getGenre() {
        // Return the raw selected genre filter.
        return genre;
    }

    public void setGenre(String genre) {
        // Store the raw selected genre filter.
        this.genre = genre;
    }

    public String getStartsWith() {
        // Return the raw startsWith browse filter.
        return startsWith;
    }

    public void setStartsWith(String startsWith) {
        // Store the raw startsWith browse filter.
        this.startsWith = startsWith;
    }

    public String getPageNumber() {
        // Return the raw requested page number.
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        // Store the raw requested page number.
        this.pageNumber = pageNumber;
    }

    public String getPageSize() {
        // Return the raw requested page size.
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        // Store the raw requested page size.
        this.pageSize = pageSize;
    }

    public String getSort1() {
        // Return the raw primary sort field.
        return sort1;
    }

    public void setSort1(String sort1) {
        // Store the raw primary sort field.
        this.sort1 = sort1;
    }

    public String getDir1() {
        // Return the raw primary sort direction.
        return dir1;
    }

    public void setDir1(String dir1) {
        // Store the raw primary sort direction.
        this.dir1 = dir1;
    }

    public String getSort2() {
        // Return the raw secondary sort field.
        return sort2;
    }

    public void setSort2(String sort2) {
        // Store the raw secondary sort field.
        this.sort2 = sort2;
    }

    public String getDir2() {
        // Return the raw secondary sort direction.
        return dir2;
    }

    public void setDir2(String dir2) {
        // Store the raw secondary sort direction.
        this.dir2 = dir2;
    }

}
