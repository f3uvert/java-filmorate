package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

@Repository
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film.Mpa> mpaRowMapper = (rs, rowNum) -> {
        Film.Mpa mpa = new Film.Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    };

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Film.Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    @Override
    public Optional<Film.Mpa> getMpaById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        try {
            Film.Mpa mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, id);
            return Optional.ofNullable(mpa);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}