package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGet(ex);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleDelete(ex);
            } else {
                ex.sendResponseHeaders(405, -1);
                ex.close();
            }
        } catch (Exception e) {
            sendJson(ex, 500, new ErrorResponse("Внутренняя ошибка сервера", null));
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String path = ex.getRequestURI().getPath();


        if (query != null && query.startsWith("year=")) {
            handleGetByYear(ex, query);
            return;
        }


        if (path.equals("/movies")) {
            List<Movie> movies = store.getAllMovies();
            sendJson(ex, 200, movies);
            return;
        }


        String[] parts = path.split("/");
        if (parts.length == 3 && parts[2].matches("\\d+")) {
            int id = Integer.parseInt(parts[2]);
            Optional<Movie> movie = store.getMovieById(id);
            if (movie.isPresent()) {
                sendJson(ex, 200, movie.get());
            } else {
                sendNotFound(ex, "Фильм не найден");
            }
            return;
        }

        sendNotFound(ex, "Страница не найдена");
    }

    private void handleGetByYear(HttpExchange ex, String query) throws IOException {
        try {
            String yearStr = query.substring(5);
            int year = Integer.parseInt(yearStr);
            List<Movie> movies = store.getMoviesByYear(year);
            sendJson(ex, 200, movies);
        } catch (NumberFormatException e) {
            sendBadRequest(ex, "Некорректный параметр запроса - 'year'");
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {

        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            ex.sendResponseHeaders(415, -1);
            ex.close();
            return;
        }


        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        try {
            Movie movie = gson.fromJson(body, Movie.class);
            List<String> errors = validateMovie(movie);

            if (!errors.isEmpty()) {
                sendJson(ex, 422, new ErrorResponse("Ошибка валидации", errors));
                return;
            }

            Movie saved = store.addMovie(movie);
            sendJson(ex, 201, saved);

        } catch (Exception e) {
            sendBadRequest(ex, "Некорректный JSON");
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (parts.length == 3 && parts[2].matches("\\d+")) {
            int id = Integer.parseInt(parts[2]);
            Optional<Movie> deleted = store.deleteMovie(id);
            if (deleted.isPresent()) {
                sendNoContent(ex);
            } else {
                sendNotFound(ex, "Фильм не найден");
            }
        } else {
            sendBadRequest(ex, "Некорректный ID");
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("Некорректные данные");
            return errors;
        }


        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }


        int currentYear = java.time.Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }

        return errors;
    }
}