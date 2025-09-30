package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import ru.yandex.practicum.filmorate.MinReleaseDate;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



@Data
public class Film {

    private int id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание фильма не может превышать 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    @PastOrPresent(message = "Дата релиза не может быть в будущем")
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    @MinReleaseDate(message = "Дата релиза — не раньше 28 декабря 1895 года")
    private int duration;

    private Mpa mpa;
    private List<Genre> genres = new ArrayList<>();

    private Set<Integer> likes = new HashSet<>();

    @Data
    public static class Mpa {
        private int id;
        private String name;
    }

    @Data
    public static class Genre {
        private int id;
        private String name;
    }
}
