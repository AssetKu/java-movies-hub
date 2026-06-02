package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListOfMoviesTypeTokenTest {

    @Test
    void listOfMovies_serialization_deserialization() {

        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie(1, "Matrix", 1999));
        movies.add(new Movie(2, "Inception", 2010));

        Gson gson = new Gson();
        String json = gson.toJson(movies);

        assertNotNull(json);
        assertTrue(json.startsWith("["));

        Type listType = new TypeToken<List<Movie>>() {}.getType();
        List<Movie> parsed = gson.fromJson(json, listType);

        assertEquals(2, parsed.size());
        assertEquals("Matrix", parsed.get(0).getTitle());
        assertEquals(2010, parsed.get(1).getYear());
    }
}