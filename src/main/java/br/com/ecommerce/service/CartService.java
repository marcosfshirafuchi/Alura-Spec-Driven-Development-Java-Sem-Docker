package br.com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.ecommerce.repository.*;
import br.com.ecommerce.model.*;
import jakarta.validation.constraints.*;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CartService {
    private final Map<String, String> carts = new ConcurrentHashMap<>();
    private final ProductRepository products;
    private final ObjectMapper mapper;

    public CartService(ProductRepository p, ObjectMapper m) {
        products = p;
        mapper = m;
    }

    public record CartItem(String productId, String name, BigDecimal price, int quantity) {
    }

    public record Cart(List<CartItem> items, BigDecimal total) {
    }

    public record AddItem(@NotBlank String productId, @Min(1) int quantity) {
    }

    private String key(String user) {
        return "cart:default:" + user;
    }

    public Cart get(String user) {
        String value = carts.get(key(user));
        try {
            return value == null ? new Cart(new ArrayList<>(), BigDecimal.ZERO) : mapper.readValue(value, Cart.class);
        } catch (Exception e) {
            throw new IllegalStateException("Carrinho inválido", e);
        }
    }

    private Cart save(String user, List<CartItem> items) {
        BigDecimal total = items.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Cart c = new Cart(items, total);
        try {
            carts.put(key(user), mapper.writeValueAsString(c));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return c;
    }

    public Cart add(String user, AddItem req) {
        Product p = products.findById(req.productId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
        if (p.status != ProductStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT, "Produto indisponível");
        List<CartItem> items = new ArrayList<>(get(user).items());
        int existing = items.stream().filter(i -> i.productId().equals(p.id)).mapToInt(CartItem::quantity).sum();
        if (p.stock < existing + req.quantity()) throw new ApiException(HttpStatus.CONFLICT, "Estoque insuficiente");
        items.removeIf(i -> i.productId().equals(p.id));
        items.add(new CartItem(p.id, p.name, p.price, existing + req.quantity()));
        return save(user, items);
    }

    public Cart remove(String user, String id) {
        List<CartItem> items = new ArrayList<>(get(user).items());
        items.removeIf(i -> i.productId().equals(id));
        return save(user, items);
    }

    public void clear(String user) {
        carts.remove(key(user));
    }
}
