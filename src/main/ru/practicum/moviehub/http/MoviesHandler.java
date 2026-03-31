package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;
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
        if (parts.length == 3) {
            String idStr = parts[2];

            try {
                int id = Integer.parseInt(idStr);
                Optional<Movie> movie = store.getMovieById(id);
                if (movie.isPresent()) {
                    sendJson(ex, 200, movie.get());
                } else {
                    sendNotFound(ex, "Фильм " + id + " не найден");
                }
            } catch (NumberFormatException e) {

                sendBadRequest(ex, "Некорректный ID");
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

        if (parts.length == 3) {
            String idStr = parts[2];

            try {
                int id = Integer.parseInt(idStr);
                Optional<Movie> deleted = store.deleteMovie(id);
                if (deleted.isPresent()) {
                    sendNoContent(ex);
                } else {
                    sendNotFound(ex, "Фильм " + id +  " не найден");
                }
            } catch (NumberFormatException e) {

                sendBadRequest(ex, "Некорректный ID: " + idStr);
            }
            return;
        }

        sendBadRequest(ex, "Некорректный ID");
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("Некорректные данные");
            return errors;
        }


        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно превышать " + MAX_TITLE_LENGTH + "символов");
        }


        int currentYear = java.time.Year.now().getValue();
        if (movie.getYear() < MIN_YEAR || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между " + MIN_YEAR + "и " + (currentYear + 1));
        }

        return errors;
    }
}