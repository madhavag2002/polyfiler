package com.project.pdfmaker.config;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientConfig {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static HttpClient getClient() {
        return CLIENT;
    }
}
