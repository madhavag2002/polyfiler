package com.project.pdfmaker.Service;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileMetadata {
    // Getters and setters
    @JsonProperty("size")
    private long size;

    @JsonProperty("upload_date")
    private String uploadDate;

    @JsonProperty("path")
    private String path;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("ttl")
    private long ttl;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("last_byte_received")
    private long lastByteReceived;

    @JsonProperty("name")
    private String name;

}
