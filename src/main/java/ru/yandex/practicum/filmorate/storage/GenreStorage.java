package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    List<Film.Genre> getAllGenres();

    Optional<Film.Genre> getGenreById(int id);
}