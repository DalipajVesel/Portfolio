package com.vesel.expensetrackerapi.controller;

import com.vesel.expensetrackerapi.entity.User;
import com.vesel.expensetrackerapi.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public void register(@RequestBody User user) {
        service.register(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody User user) {
        AuthResponse response = new AuthResponse();
        response.token = service.login(user);
        return response;
    }
}
