package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import br.com.ecommerce.service.AuthService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService s) {
        service = s;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Object register(@Valid @RequestBody AuthService.Register r) {
        return Map.of("data", service.register(r));
    }

    @PostMapping("/login")
    public Object login(@Valid @RequestBody AuthService.Login r) {
        return Map.of("data", service.login(r));
    }
}
