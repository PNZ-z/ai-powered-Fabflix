package com.fabflix.boot.search;

public class SearchRequest {
    private String query;
    private String pageNumber;
    private String pageSize;
    private String sort1;
    private String dir1;
    private String sort2;
    private String dir2;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getPageNumber() { return pageNumber; }
    public void setPageNumber(String pageNumber) { this.pageNumber = pageNumber; }
    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }
    public String getSort1() { return sort1; }
    public void setSort1(String sort1) { this.sort1 = sort1; }
    public String getDir1() { return dir1; }
    public void setDir1(String dir1) { this.dir1 = dir1; }
    public String getSort2() { return sort2; }
    public void setSort2(String sort2) { this.sort2 = sort2; }
    public String getDir2() { return dir2; }
    public void setDir2(String dir2) { this.dir2 = dir2; }
}
