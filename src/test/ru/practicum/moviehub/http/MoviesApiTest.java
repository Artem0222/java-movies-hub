package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new GsonBuilder().create();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));

        Movie[] movies = gson.fromJson(body, Movie[].class);
        assertEquals(0, movies.length);
    }

    @Test
    void postMovie_whenValid_returnsCreated() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setYear(2023);

        String json = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode());

        Movie created = gson.fromJson(response.body(), Movie.class);
        assertNotNull(created.getId());
        assertEquals("Test Movie", created.getTitle());
        assertEquals(2023, created.getYear());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        // Сначала добавляем фильм
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setYear(2023);

        String json = gson.toJson(movie);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResponse.body(), Movie.class);
        int id = created.getId();


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode());

        Movie found = gson.fromJson(getResponse.body(), Movie.class);
        assertEquals(id, found.getId());
        assertEquals("Test Movie", found.getTitle());
    }

    @Test
    void deleteMovie_whenExists_returnsNoContent() throws Exception {

        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setYear(2023);

        String json = gson.toJson(movie);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResponse.body(), Movie.class);
        int id = created.getId();


        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, deleteResponse.statusCode());


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResponse.statusCode());
    }
}