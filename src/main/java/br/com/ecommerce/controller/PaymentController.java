package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import br.com.ecommerce.service.PaymentService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService s) {
        service = s;
    }

    public record CreatePayment(String orderId) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreatePayment r) {
        return Map.of("data", service.create(r.orderId(), jwt.getSubject()));
    }

    @PostMapping("/{id}/confirm")
    public Object confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return Map.of("data", service.confirm(id, jwt.getSubject(), key));
    }

    @PostMapping("/{id}/cancel")
    public Object cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return Map.of("data", service.cancel(id, jwt.getSubject()));
    }
}
