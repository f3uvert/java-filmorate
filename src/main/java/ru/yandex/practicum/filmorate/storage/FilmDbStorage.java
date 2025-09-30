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

    private JdbcTemplate jdbcTemplate;

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


        Film.Mpa mpa = new Film.Mpa();
        try {
            int mpaId = rs.getInt("mpa_id");
            if (!rs.wasNull() && mpaId > 0) {
                mpa.setId(mpaId);
                try {
                    String mpaName = jdbcTemplate.queryForObject(
                            "SELECT name FROM mpa_ratings WHERE id = ?",
                            String.class, mpaId
                    );
                    mpa.setName(mpaName != null ? mpaName : "Unknown");
                } catch (EmptyResultDataAccessException e) {
                    mpa.setName("Unknown");
                }
            } else {
                mpa.setId(1);
                mpa.setName("G");
            }
        } catch (Exception e) {
            mpa.setId(1);
            mpa.setName("G");
        }
        film.setMpa(mpa);

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
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Film.Genre genre = new Film.Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                return genre;
            }, filmId);
        } catch (Exception e) {
            System.err.println("Error getting genres for film " + filmId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> uniqueGenreIds = film.getGenres().stream()
                    .map(Film.Genre::getId)
                    .collect(Collectors.toSet());

            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Integer genreId : uniqueGenreIds) {
                jdbcTemplate.update(sql, film.getId(), genreId);
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
        validateMpaExists(film.getMpa().getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateGenresExist(film.getGenres());
        }

        if (film.getMpa() == null) {
            Film.Mpa defaultMpa = new Film.Mpa();
            defaultMpa.setId(1);
            film.setMpa(defaultMpa);
        }

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
        saveGenres(film);
        saveLikes(film);

        return film;
    }

    @Override
    public Film update(Film film) {
        getById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id " + film.getId() + " не найден"));

        validateMpaExists(film.getMpa().getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateGenresExist(film.getGenres());
        }

        if (film.getMpa() == null) {
            Film.Mpa defaultMpa = new Film.Mpa();
            defaultMpa.setId(1);
            film.setMpa(defaultMpa);
        }

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

        updateGenres(film);
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
        } catch (Exception e) {
            System.err.println("Error getting film by id " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
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

    private void validateMpaExists(int mpaId) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mpaId);

        if (count == null || count == 0) {
            throw new NotFoundException("MPA рейтинг с id " + mpaId + " не найден");
        }
    }

    private void validateGenresExist(List<Film.Genre> genres) {
        Set<Integer> genreIds = genres.stream()
                .map(Film.Genre::getId)
                .collect(Collectors.toSet());

        String sql = "SELECT COUNT(*) FROM genres WHERE id IN (" +
                genreIds.stream().map(String::valueOf).collect(Collectors.joining(",")) +
                ")";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        if (count == null || count != genreIds.size()) {
            throw new NotFoundException("Один или несколько жанров не найдены");
        }
    }
}