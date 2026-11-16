package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client for the Rust microservice (main.rs). Used by ALGS4Middleware.Middleware
 * when Engine.RUST is selected. Same HttpClient approach as ComputeResource.
 * Base URL: env RUST_SERVICE_URL, default http://localhost:8081
 */
public class RustBackend {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static String baseUrl() {
        String u = System.getenv("RUST_SERVICE_URL");
        return (u == null || u.isBlank()) ? "http://localhost:8081" : u;
    }

    /** Sorts arr in place using the Rust service. algo = "heap" | "merge" | "quick". */
    public static void sort(Integer[] arr, String algo, boolean descending) {
        String body = "{\"values\":" + toJson(arr)
                + ",\"algo\":\"" + algo + "\""
                + ",\"descending\":" + descending + "}";
        Integer[] out = fromJson(post("/sort", body));
        if (out.length != arr.length)
            throw new IllegalStateException("Rust service returned " + out.length + " values, expected " + arr.length);
        System.arraycopy(out, 0, arr, 0, arr.length);
    }

    /** k-th smallest (0-based) via the Rust service. Does not modify arr. */
    public static Integer select(Integer[] arr, int k) {
        String body = "{\"values\":" + toJson(arr) + ",\"k\":" + k + "}";
        return Integer.valueOf(post("/select", body).trim());
    }

    private static String post(String path, String json) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 400)
                throw new IllegalArgumentException(res.body());
            if (res.statusCode() / 100 != 2)
                throw new IllegalStateException("Rust service error " + res.statusCode() + ": " + res.body());
            return res.body();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reach Rust service at " + baseUrl() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Rust service", e);
        }
    }

    private static String toJson(Integer[] a) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.append(']').toString();
    }

    private static Integer[] fromJson(String s) {
        s = s.trim();
        if (!s.startsWith("[") || !s.endsWith("]"))
            throw new IllegalStateException("Unexpected response from Rust service: " + s);
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return new Integer[0];
        String[] parts = s.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.valueOf(parts[i].trim());
        return out;
    }
}
