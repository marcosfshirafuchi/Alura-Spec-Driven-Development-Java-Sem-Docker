package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import br.com.ecommerce.service.CartService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService s) {
        service = s;
    }

    @GetMapping
    public Object get(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("data", service.get(jwt.getSubject()));
    }

    @PostMapping("/items")
    public Object add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartService.AddItem r) {
        return Map.of("data", service.add(jwt.getSubject(), r));
    }

    @DeleteMapping("/items/{itemId}")
    public Object remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String itemId) {
        return Map.of("data", service.remove(jwt.getSubject(), itemId));
    }
}
