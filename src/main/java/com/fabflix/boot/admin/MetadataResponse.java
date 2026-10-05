package com.fabflix.boot.admin;

import java.util.List;

public class MetadataResponse {
    private final String status = "success";
    private final List<MetadataTable> tables;

    public MetadataResponse(List<MetadataTable> tables) { this.tables = tables; }
    public String getStatus() { return status; }
    public List<MetadataTable> getTables() { return tables; }
}
