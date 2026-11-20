package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import br.com.ecommerce.service.OrderService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService s) {
        service = s;
    }

    @GetMapping
    public Object list(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("data", service.list(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object checkout(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("data", service.checkout(jwt.getSubject()));
    }

    @PostMapping("/{id}/cancel")
    public Object cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        throw new UnsupportedOperationException("Não implementado");
    }
}
