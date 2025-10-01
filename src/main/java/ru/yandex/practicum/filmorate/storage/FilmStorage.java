package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    List<Film> getAll();

    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(int id);

    void delete(int id);

    List<Film> getPopular(int count);

}