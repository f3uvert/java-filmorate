package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

@Service
public class GenreService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GenreService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film.Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film.Genre genre = new Film.Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            return genre;
        });
    }

    public Film.Genre getGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Film.Genre genre = new Film.Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                return genre;
            }, id);
        } catch (Exception e) {
            throw new NotFoundException("Жанр с id " + id + " не найден");
        }
    }
}
