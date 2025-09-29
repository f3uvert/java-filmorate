package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

@Service
public class MpaService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film.Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film.Mpa mpa = new Film.Mpa();
            mpa.setId(rs.getInt("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        });
    }

    public Film.Mpa getMpaById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Film.Mpa mpa = new Film.Mpa();
                mpa.setId(rs.getInt("id"));
                mpa.setName(rs.getString("name"));
                return mpa;
            }, id);
        } catch (Exception e) {
            throw new NotFoundException("MPA рейтинг с id " + id + " не найден");
        }
    }
}