package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
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
    private LikeStorage likeStorage;
    private GenreStorage genreStorage;

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

        Set<Integer> likes = new HashSet<>(likeStorage.getLikes(film.getId()));
        film.setLikes(likes);

        List<Film.Genre> genres = genreStorage.getGenresForFilm(film.getId());
        film.setGenres(genres);

        Film.Mpa mpa = new Film.Mpa();
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
        film.setMpa(mpa);

        return film;
    };

    public FilmDbStorage(JdbcTemplate jdbcTemplate, LikeStorage likeStorage, GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.likeStorage = likeStorage;
        this.genreStorage = genreStorage;
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

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            genreStorage.saveGenresForFilm(film.getId(), film.getGenres());
        }

        if (film.getLikes() != null && !film.getLikes().isEmpty()) {
            likeStorage.saveAllLikes(film.getId(), new ArrayList<>(film.getLikes()));
        }

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

        List<Film.Genre> genresToUpdate = film.getGenres() != null ? film.getGenres() : Collections.emptyList();
        genreStorage.updateGenresForFilm(film.getId(), genresToUpdate);

        List<Integer> likesToUpdate = film.getLikes() != null ? new ArrayList<>(film.getLikes()) : Collections.emptyList();
        likeStorage.updateLikes(film.getId(), likesToUpdate);

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
            throw new RuntimeException("Error getting film by id " + id, e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void addLike(int filmId, int userId) {
        likeStorage.addLike(filmId, userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        likeStorage.removeLike(filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = "SELECT f.*, " +
                "COALESCE(like_counts.like_count, 0) as likes_count " +
                "FROM films f " +
                "LEFT JOIN (" +
                "    SELECT film_id, COUNT(user_id) as like_count " +
                "    FROM likes " +
                "    GROUP BY film_id" +
                ") like_counts ON f.id = like_counts.film_id " +
                "ORDER BY likes_count DESC, f.id " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, filmRowMapper, count);
    }

    private void validateMpaExists(int mpaId) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mpaId);

        if (count == null || count == 0) {
            throw new NotFoundException("MPA рейтинг с id " + mpaId + " не найден");
        }
    }

    private void validateGenresExist(List<Film.Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        Set<Integer> genreIds = genres.stream()
                .map(Film.Genre::getId)
                .collect(Collectors.toSet());

        String placeholders = String.join(",", Collections.nCopies(genreIds.size(), "?"));
        String sql = "SELECT COUNT(*) FROM genres WHERE id IN (" + placeholders + ")";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genreIds.toArray());

        if (count == null || count != genreIds.size()) {
            throw new NotFoundException("Один или несколько жанров не найдены");
        }
    }
}