package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Service
public class MpaService {

    private final MpaStorage mpaStorage;

    @Autowired
    public MpaService(MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    public List<Film.Mpa> getAllMpa() {
        return mpaStorage.getAllMpa();
    }

    public Film.Mpa getMpaById(int id) {
        return mpaStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("MPA рейтинг с id " + id + " не найден"));
    }
}