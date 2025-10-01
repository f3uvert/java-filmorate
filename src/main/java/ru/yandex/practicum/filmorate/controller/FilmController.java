package ru.yandex.practicum.filmorate.controller;



import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    private final FilmService filmService;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public Film createFilm(@Valid @RequestBody Film film) {
        log.info("Получен запрос на создание фильма: {}", film);
        Film createdFilm = filmService.create(film);
        log.info("Фильм успешно создан: {}", createdFilm);
        return createdFilm;

    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        Film updatedFilm = filmService.update(film);
        log.info("Фильм успешно обновлен: {}", updatedFilm);
        return updatedFilm;

    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов");

        return filmService.getAll();

    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable int id) {
        log.info("Получен запрос на получение фильма с id: {}", id);
        return filmService.getById(id);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Название фильма не может быть пустым");
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Описание фильма превышает 200 символов");
            throw new ValidationException("Описание фильма не может превышать 200 символов");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза должна быть не раньше {}", MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза должна быть не раньше " + MIN_RELEASE_DATE);
        }

        if (film.getDuration() <= 0) {
            log.warn("Продолжительность фильма должна быть положительным числом");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }


    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Получен запрос на добавление лайка фильму {} от пользователя {}", id, userId);
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Получен запрос на удаление лайка фильму {} от пользователя {}", id, userId);
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopular(@RequestParam(defaultValue = "10") int count) {
        log.info("Получен запрос на получение {} популярных фильмов", count);
        return filmService.getPopular(count);
    }


}