package ru.yandex.practicum.filmorate.storage;

import java.util.List;

public interface LikeStorage {
    void addLike(int filmId, int userId);

    void removeLike(int filmId, int userId);

    List<Integer> getLikes(int filmId);

    void saveAllLikes(int filmId, List<Integer> userIds);

    void updateLikes(int filmId, List<Integer> userIds);

    int getLikesCount(int filmId);

    boolean existsLike(int filmId, int userId);
}