package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreStorage = genreStorage;
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));

        Date releaseDate = rs.getDate("release_date");
        if (releaseDate != null) {
            film.setReleaseDate(releaseDate.toLocalDate());
        }

        film.setDuration(rs.getInt("duration"));

        Film.Mpa mpa = new Film.Mpa();
        mpa.setId(rs.getInt("mpa_id"));
        mpa.setName(rs.getString("mpa_name"));
        film.setMpa(mpa);

        return film;
    };

    @Override
    public List<Film> getAll() {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_id, m.name as mpa_name, " +
                "COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id', g.id, 'name', g.name)) " +
                "          FROM film_genres fg " +
                "          JOIN genres g ON fg.genre_id = g.id " +
                "          WHERE fg.film_id = f.id), JSON_ARRAY()) as genres " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "ORDER BY f.id";
        return jdbcTemplate.query(sql, filmRowMapper);
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null);
            stmt.setInt(4, film.getDuration());
            stmt.setInt(5, film.getMpa().getId());
            return stmt;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        // Сохраняем жанры после создания фильма
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreStorage.saveGenresForFilm(film.getId(), film.getGenres());
        }

        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null,
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        if (updated == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        updateGenresForFilm(film.getId(), film.getGenres());
        return film;
    }

    @Override
    public Optional<Film> getById(int id) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_id, m.name as mpa_name, " +
                "COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id', g.id, 'name', g.name)) " +
                "          FROM film_genres fg " +
                "          JOIN genres g ON fg.genre_id = g.id " +
                "          WHERE fg.film_id = f.id), JSON_ARRAY()) as genres " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            return Optional.of(film);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_id, m.name as mpa_name, " +
                "COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id', g.id, 'name', g.name)) " +
                "          FROM film_genres fg " +
                "          JOIN genres g ON fg.genre_id = g.id " +
                "          WHERE fg.film_id = f.id), JSON_ARRAY()) as genres, " +
                "COALESCE(l.like_count, 0) as like_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN (" +
                "    SELECT film_id, COUNT(user_id) as like_count " +
                "    FROM likes " +
                "    GROUP BY film_id" +
                ") l ON f.id = l.film_id " +
                "ORDER BY like_count DESC, f.id " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, filmRowMapper, count);
    }

    private void saveGenresForFilm(int filmId, List<Film.Genre> genres) {
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

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
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

    private void updateGenresForFilm(int filmId, List<Film.Genre> genres) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, filmId);

        saveGenresForFilm(filmId, genres);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}