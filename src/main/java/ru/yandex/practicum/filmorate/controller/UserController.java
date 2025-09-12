package ru.yandex.practicum.filmorate.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);

        validateUser(user);


        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(nextId++);
        users.put(user.getId(), user);

        log.info("Пользователь успешно создан: {}", user);
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        if (user.getId() <= 0 || !users.containsKey(user.getId())) {
            log.warn("Пользователь с id {} не найден", user.getId());
            throw new ValidationException("Пользователь с указанным id не найден");
        }

        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        users.put(user.getId(), user);
        log.info("Пользователь успешно обновлен: {}", user);
        return user;
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей");
        return new ArrayList<>(users.values());
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Email должен содержать символ @");
            throw new ValidationException("Email должен содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Логин не может быть пустым");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.warn("Логин не может содержать пробелы");
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения не может быть в будущем");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}