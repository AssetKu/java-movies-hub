package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @Test
    @Order(1)
    void getEmptyMovies() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim());
    }

    @Test
    @Order(2)
    void createMovie_success() throws Exception {
        String json = "{\"title\":\"Matrix\",\"year\":1999}";

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("Matrix"));
    }

    @Test
    @Order(3)
    void createMovie_invalid() throws Exception {
        String json = "{\"title\":\"\",\"year\":1000}";

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(422, resp.statusCode());
    }

    @Test
    @Order(4)
    void createMovie_wrongContentType() throws Exception {

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(415, resp.statusCode());
    }

    @Test
    @Order(5)
    void getMovieById() throws Exception {

        // создаём
        client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Matrix\",\"year\":1999}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies/1"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Matrix"));
    }

    @Test
    @Order(6)
    void getMovie_notFound() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies/999"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(404, resp.statusCode());
    }

    @Test
    @Order(7)
    void deleteMovie() throws Exception {

        client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Test\",\"year\":2000}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies/1"))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(204, resp.statusCode());
    }

    @Test
    @Order(8)
    void filterByYear() throws Exception {

        client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Test\",\"year\":2020}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies?year=2020"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("2020"));
    }
}