package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    public static void main(String[] args) {
        MoviesStore store = new MoviesStore();
        MoviesServer server = new MoviesServer(store, 8080);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.println("MovieHub API запущен!");
        System.out.println("Доступные эндпоинты:");
        System.out.println("GET    /movies");
        System.out.println("GET    /movies?year=YYYY");
        System.out.println("GET    /movies/{id}");
        System.out.println("POST   /movies");
        System.out.println("DELETE /movies/{id}");
    }
}