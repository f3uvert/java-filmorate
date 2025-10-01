package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film.Genre> genreRowMapper = (rs, rowNum) -> {
        Film.Genre genre = new Film.Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    };

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Film.Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, genreRowMapper);
    }

    @Override
    public Optional<Film.Genre> getGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE id = ?";
        try {
            Film.Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper, id);
            return Optional.ofNullable(genre);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Film.Genre> getGenresForFilm(int filmId) {
        String sql = "SELECT g.id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.id";

        System.out.println("Getting genres for film " + filmId);

        List<Film.Genre> genres = jdbcTemplate.query(sql, genreRowMapper, filmId);
        System.out.println("Found genres: " + genres.size());

        return genres;
    }

    public void saveGenresForFilm(int filmId, List<Film.Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        Set<Film.Genre> uniqueGenres = genres.stream()
                .collect(Collectors.toMap(
                        Film.Genre::getId,
                        genre -> genre,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toSet());

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                Film.Genre genre = new ArrayList<>(uniqueGenres).get(i);
                ps.setInt(1, filmId);
                ps.setInt(2, genre.getId());
            }

            @Override
            public int getBatchSize() {
                return uniqueGenres.size();
            }
        });
    }

    public void updateGenresForFilm(int filmId, List<Film.Genre> genres) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, filmId);
        saveGenresForFilm(filmId, genres);
    }
}