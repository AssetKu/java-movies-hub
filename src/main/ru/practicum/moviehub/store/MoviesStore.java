package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {

    private final Map<Long, Movie> storage = new HashMap<>();
    private long nextId = 1;

    public List<Movie> getAll() {
        return new ArrayList<>(storage.values());
    }

    public Movie add(String title, int year) {
        Movie m = new Movie(nextId++, title, year);
        storage.put(m.getId(), m);
        return m;
    }

    public Optional<Movie> getById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public boolean delete(long id) {
        return storage.remove(id) != null;
    }

    public List<Movie> getByYear(int year) {
        List<Movie> res = new ArrayList<>();
        for (Movie m : storage.values()) {
            if (m.getYear() == year) res.add(m);
        }
        return res;
    }

    public void clear() {
        storage.clear();
        nextId = 1;
    }
}