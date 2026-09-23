package com.vesel.expensetrackerapi.service;

import com.vesel.expensetrackerapi.entity.User;
import com.vesel.expensetrackerapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository repository;
    private final JwtService jwtService;

    public AuthService(UserRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public void register(User user) {
        if (user.username == null || user.username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Optional<User> existing = repository.findByUsername(user.username);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        User saved = new User();
        saved.username = user.username;
        repository.save(saved);
    }

    public String login(User user) {
        Optional<User> found = repository.findByUsername(user.username);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return jwtService.createToken(found.get().username);
    }
}
