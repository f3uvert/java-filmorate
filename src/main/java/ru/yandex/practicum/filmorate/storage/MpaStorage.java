package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;
import java.util.Optional;

public interface MpaStorage {
    List<Film.Mpa> getAllMpa();
    Optional<Film.Mpa> getMpaById(int id);
}