package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.GenreService;

import java.util.List;

@RestController
@RequestMapping("/genres")
@Slf4j
public class GenreController {

    private final GenreService genreService;

    @Autowired
    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public List<Film.Genre> getAllGenres() {
        log.info("Получен запрос на получение всех жанров");
        return genreService.getAllGenres();
    }

    @GetMapping("/{id}")
    public Film.Genre getGenreById(@PathVariable int id) {
        log.info("Получен запрос на получение жанра с id: {}", id);
        return genreService.getGenreById(id);
    }
}
