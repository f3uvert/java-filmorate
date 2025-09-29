package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FilmorateApplicationTests {

    @Test
    void testFilmValidation() {
        Film film = new Film();

        film.setName("");
        film.setDescription("Test description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

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
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание фильма не может превышать 200 символов");
        }

        LocalDate minDate = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(minDate)) {
            throw new ValidationException("Дата релиза должна быть не раньше " + minDate);
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Email должен содержать символ @");
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
    }


}


