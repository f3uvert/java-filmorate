package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;


import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FilmorateApplicationTests {

    @Test
    void testFilmValidation() {
        Film film = new Film();
        Film.Mpa mpa = new Film.Mpa();
        mpa.setId(1);


        film.setName("");
        film.setDescription("Test description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(mpa);
        film.setGenres(new ArrayList<>());

        assertThrows(ValidationException.class, () -> validateFilm(film));


        film.setName("Test Film");
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> validateFilm(film));


        film.setDescription("Normal description");
        film.setReleaseDate(LocalDate.of(1890, 1, 1));

        assertThrows(ValidationException.class, () -> validateFilm(film));


        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-10);

        assertThrows(ValidationException.class, () -> validateFilm(film));


        film.setDuration(0);

        assertThrows(ValidationException.class, () -> validateFilm(film));
    }

    @Test
    void testUserValidation() {
        User user = new User();


        user.setEmail("invalid-email");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> validateUser(user));


        user.setEmail("test@example.com");
        user.setLogin("");

        assertThrows(ValidationException.class, () -> validateUser(user));


        user.setLogin("login with spaces");

        assertThrows(ValidationException.class, () -> validateUser(user));


        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> validateUser(user));


        user.setLogin("validlogin");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));


        validateUser(user);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        LocalDate minDate = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(minDate)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }


        if (film.getMpa() != null && film.getMpa().getId() > 0) {

            if (film.getMpa().getId() < 1 || film.getMpa().getId() > 5) {
                throw new NotFoundException("MPA рейтинг с id " + film.getMpa().getId() + " не найден");
            }
        }
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }


        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    @Test
    void testFilmWithMpaAndGenres() {
        Film film = new Film();
        Film.Mpa mpa = new Film.Mpa();
        mpa.setId(1);

        Film.Genre genre = new Film.Genre();
        genre.setId(1);

        film.setName("Test Film");
        film.setDescription("Test description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(mpa);
        film.setGenres(java.util.Arrays.asList(genre));


        validateFilm(film);
    }

    @Test
    void testFilmWithInvalidMpa() {
        Film film = new Film();
        Film.Mpa mpa = new Film.Mpa();
        mpa.setId(10);

        film.setName("Test Film");
        film.setDescription("Test description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(mpa);
        film.setGenres(new ArrayList<>());

        assertThrows(NotFoundException.class, () -> validateFilm(film));
    }


}