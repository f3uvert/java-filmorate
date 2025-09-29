package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Primary
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        Set<Integer> likes = getLikesIds(film.getId());
        film.setLikes(likes);

        List<Film.Genre> genres = getGenresForFilm(film.getId());
        film.setGenres(genres);

        return film;
    };

    private Film.Mpa getMpaById(int mpaId) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Film.Mpa mpa = new Film.Mpa();
                mpa.setId(rs.getInt("id"));
                mpa.setName(rs.getString("name"));
                return mpa;
            }, mpaId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private List<Film.Genre> getGenresForFilm(int filmId) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film.Genre genre = new Film.Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId);
    }

    private void saveGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Film.Genre genre : film.getGenres()) {
                jdbcTemplate.update(sql, film.getId(), genre.getId());
            }
        }
    }

    private void updateGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        saveGenres(film);
    }

    @Override
    public List<Film> getAll() {
        String sql = "SELECT * FROM films ORDER BY id";
        return jdbcTemplate.query(sql, filmRowMapper);
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null);
            stmt.setInt(4, film.getDuration());
            return stmt;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        saveLikes(film);

        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null,
                film.getDuration(),
                film.getId());

        if (updated == 0) {
            throw new RuntimeException("Фильм с id " + film.getId() + " не найден");
        }

        updateLikes(film);

        return film;
    }

    @Override
    public Optional<Film> getById(int id) {
        String sql = "SELECT * FROM films WHERE id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void addLike(int filmId, int userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = "SELECT f.*, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, filmRowMapper, count);
    }

    private Set<Integer> getLikesIds(Integer filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("user_id"), filmId));
    }

    private void saveLikes(Film film) {
        if (film.getLikes() != null && !film.getLikes().isEmpty()) {
            String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
            for (Integer userId : film.getLikes()) {
                jdbcTemplate.update(sql, film.getId(), userId);
            }
        }
    }

    private void updateLikes(Film film) {
        String deleteSql = "DELETE FROM likes WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());
        saveLikes(film);
    }
}