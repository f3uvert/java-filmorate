package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Slf4j
public class MpaController {

    private final MpaService mpaService;

    @Autowired
    public MpaController(MpaService mpaService) {
        this.mpaService = mpaService;
    }

    @GetMapping
    public List<Film.Mpa> getAllMpa() {
        log.info("Получен запрос на получение всех MPA рейтингов");
        return mpaService.getAllMpa();
    }

    @GetMapping("/{id}")
    public Film.Mpa getMpaById(@PathVariable int id) {
        log.info("Получен запрос на получение MPA рейтинга с id: {}", id);
        return mpaService.getMpaById(id);
    }
}