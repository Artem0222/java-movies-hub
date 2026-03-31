package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    protected void sendJson(HttpExchange ex, int status, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    protected void sendNotFound(HttpExchange ex, String message) throws IOException {
        sendJson(ex, 404, new ru.practicum.moviehub.api.ErrorResponse(message, null));
    }

    protected void sendBadRequest(HttpExchange ex, String message) throws IOException {
        sendJson(ex, 400, new ru.practicum.moviehub.api.ErrorResponse(message, null));
    }
}