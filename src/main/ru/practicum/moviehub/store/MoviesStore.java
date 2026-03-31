package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public Movie addMovie(Movie movie) {
        int id = nextId.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public Optional<Movie> getMovieById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Optional<Movie> deleteMovie(int id) {
        return Optional.ofNullable(movies.remove(id));
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public void clear() {
        movies.clear();
        nextId.set(1);
    }
}