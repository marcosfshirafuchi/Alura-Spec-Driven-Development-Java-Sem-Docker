package br.com.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import br.com.ecommerce.service.CatalogService;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService s) {
        service = s;
    }

    @GetMapping("/categories")
    public Object categories() {
        return Map.of("data", service.categories());
    }

    @GetMapping("/categories/{id}")
    public Object category(@PathVariable String id) {
        return Map.of("data", service.category(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createCategory(@Valid @RequestBody CatalogService.CategoryInput r) {
        return Map.of("data", service.saveCategory(null, r));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/categories/{id}")
    public Object updateCategory(@PathVariable String id, @Valid @RequestBody CatalogService.CategoryInput r) {
        return Map.of("data", service.saveCategory(id, r));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable String id) {
        service.deleteCategory(id);
    }

    @GetMapping("/products")
    public Object products(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String search, @RequestParam(required = false) String categoryId) {
        return service.products(page, limit, search, categoryId);
    }

    @GetMapping("/products/{id}")
    public Object product(@PathVariable String id) {
        return Map.of("data", service.product(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createProduct(@Valid @RequestBody CatalogService.ProductInput r) {
        return Map.of("data", service.saveProduct(null, r));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/products/{id}")
    public Object updateProduct(@PathVariable String id, @Valid @RequestBody CatalogService.ProductInput r) {
        return Map.of("data", service.saveProduct(id, r));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String id) {
        service.deleteProduct(id);
    }
}
