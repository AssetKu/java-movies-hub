package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        if (method.equalsIgnoreCase("GET") && path.equals("/movies")) {

            if (query != null && query.startsWith("year=")) {
                try {
                    int year = Integer.parseInt(query.split("=")[1]);
                    sendMovies(store.getByYear(year), ex);
                } catch (Exception e) {
                    sendJson(ex, 400, error("Некорректный параметр запроса — 'year'"));
                }
                return;
            }

            sendMovies(store.getAll(), ex);
            return;
        }

        if (method.equalsIgnoreCase("GET") && path.startsWith("/movies/")) {
            try {
                long id = Long.parseLong(path.substring(8));

                var movie = store.getById(id);
                if (movie.isEmpty()) {
                    sendJson(ex, 404, error("Фильм не найден"));
                } else {
                    sendJson(ex, 200, toJson(movie.get()));
                }

            } catch (Exception e) {
                sendJson(ex, 400, error("Некорректный ID"));
            }
            return;
        }

        if (method.equalsIgnoreCase("DELETE") && path.startsWith("/movies/")) {
            try {
                long id = Long.parseLong(path.substring(8));

                if (store.delete(id)) {
                    sendNoContent(ex);
                } else {
                    sendJson(ex, 404, error("Фильм не найден"));
                }

            } catch (Exception e) {
                sendJson(ex, 400, error("Некорректный ID"));
            }
            return;
        }

        if (method.equalsIgnoreCase("POST") && path.equals("/movies")) {

            String ct = ex.getRequestHeaders().getFirst("Content-Type");

            if (ct == null || !ct.contains("application/json")) {
                sendJson(ex, 415, error("Unsupported Media Type"));
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            String title;
            Integer year;

            try {
                title = extractString(body, "title");
                year = extractInt(body, "year");
            } catch (Exception e) {
                sendJson(ex, 400, error("Некорректный JSON"));
                return;
            }

            List<String> errors = new ArrayList<>();

            if (title == null || title.isBlank()) {
                errors.add("название не должно быть пустым");
            }

            if (title != null && title.length() > 100) {
                errors.add("длина ≤ 100");
            }

            int currentYear = Year.now().getValue();

            if (year == null || year < 1888 || year > currentYear + 1) {
                errors.add("год вне диапазона");
            }

            if (!errors.isEmpty()) {
                sendJson(ex, 422, errorDetails(errors));
                return;
            }

            Movie m = store.add(title, year);
            sendJson(ex, 201, toJson(m));
            return;
        }

        sendJson(ex, 405, error("Method Not Allowed"));
    }

    private void sendMovies(List<Movie> list, HttpExchange ex) throws IOException {
        if (list.isEmpty()) {
            sendJson(ex, 200, "[]");
            return;
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            json.append(toJson(list.get(i)));
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        sendJson(ex, 200, json.toString());
    }

    private String toJson(Movie m) {
        return "{\"id\":" + m.getId() + ",\"title\":\"" + m.getTitle() + "\",\"year\":" + m.getYear() + "}";
    }

    private String error(String msg) {
        return "{\"error\":\"" + msg + "\"}";
    }

    private String errorDetails(List<String> list) {
        return "{\"error\":\"Ошибка валидации\",\"details\":" + list.toString() + "}";
    }

    private String extractString(String json, String field) {
        int i = json.indexOf("\"" + field + "\"");
        int start = json.indexOf("\"", i + field.length() + 2) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private Integer extractInt(String json, String field) {
        int i = json.indexOf("\"" + field + "\"");
        int colon = json.indexOf(":", i);
        int end = json.indexOf(",", colon);

        if (end == -1) end = json.indexOf("}", colon);

        return Integer.parseInt(json.substring(colon + 1, end).trim());
    }
}