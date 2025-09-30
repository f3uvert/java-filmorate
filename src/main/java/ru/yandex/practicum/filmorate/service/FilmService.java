package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.NotFoundException;
import ru.yandex.practicum.filmorate.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaService mpaService;
    private final GenreService genreService;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage,
                       MpaService mpaService, GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());
        if (film.getMpa() == null) {
            Film.Mpa defaultMpa = new Film.Mpa();
            defaultMpa.setId(1);
            film.setMpa(defaultMpa);
        }
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());
        if (film.getMpa() == null) {
            Film.Mpa defaultMpa = new Film.Mpa();
            defaultMpa.setId(1);
            film.setMpa(defaultMpa);
        }
        return filmStorage.update(film);
    }

    public Film getById(int id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public void addLike(int filmId, int userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + filmId + " не найден"));

        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + filmId + " не найден"));

        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getPopular(count);
    }

    private void validateMpa(Film.Mpa mpa) {
        if (mpa == null || mpa.getId() == 0) {
            return;
        }

        try {
            mpaService.getMpaById(mpa.getId());
        } catch (NotFoundException e) {
            throw new NotFoundException("MPA рейтинг с id " + mpa.getId() + " не найден");
        }
    }

    private void validateGenres(List<Film.Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        for (Film.Genre genre : genres) {
            try {
                genreService.getGenreById(genre.getId());
            } catch (NotFoundException e) {
                throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
            }
        }
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
    }
}