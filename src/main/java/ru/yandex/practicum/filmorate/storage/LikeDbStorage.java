package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class LikeDbStorage implements LikeStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public LikeDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addLike(int filmId, int userId) {
        String checkSql = "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, userId);

        if (count == null || count == 0) {
            String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, filmId, userId);
        }
    }

    @Override
    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Integer> getLikes(int filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ? ORDER BY user_id";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("user_id"), filmId);
    }

    @Override
    public void saveAllLikes(int filmId, List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, filmId);
                ps.setInt(2, userIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return userIds.size();
            }
        });
    }

    @Override
    public void updateLikes(int filmId, List<Integer> userIds) {
        String deleteSql = "DELETE FROM likes WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, filmId);

        saveAllLikes(filmId, userIds);
    }

    @Override
    public int getLikesCount(int filmId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    @Override
    public boolean existsLike(int filmId, int userId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        return count != null && count > 0;
    }
}