package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import br.com.ecommerce.service.UserService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    public UserController(UserService s) {
        service = s;
    }

    @GetMapping("/{id}")
    public Object get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return Map.of("data", service.get(id, jwt.getSubject(), jwt.getClaimAsString("role")));
    }

    @PutMapping("/{id}")
    public Object update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @RequestBody UserService.UpdateUser r) {
        return Map.of("data", service.update(id, r, jwt.getSubject(), jwt.getClaimAsString("role")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        service.delete(id, jwt.getSubject(), jwt.getClaimAsString("role"));
    }
}
