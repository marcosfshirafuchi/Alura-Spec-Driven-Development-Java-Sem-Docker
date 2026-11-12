package br.com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public CatalogService(CategoryRepository c, ProductRepository p) {
        categories = c;
        products = p;
    }

    public record CategoryInput(@NotBlank String name, String parentId) {
    }

    public record ProductInput(@NotBlank String name, @NotBlank String description, @NotBlank String sku,
                               @NotNull @DecimalMin("0.0") BigDecimal price, @Min(0) int stock, ProductStatus status,
                               @NotBlank String categoryId) {
    }

    public Category category(String id) {
        return categories.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
    }

    public Product product(String id) {
        return products.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }

    public List<Category> categories() {
        return categories.findAll();
    }

    public Category saveCategory(String id, CategoryInput r) {
        Category c = id == null ? new Category() : category(id);
        c.name = r.name();
        c.parent = r.parentId() == null ? null : category(r.parentId());
        c.updatedAt = Instant.now();
        return categories.save(c);
    }

    public void deleteCategory(String id) {
        categories.delete(category(id));
    }

    public Product saveProduct(String id, ProductInput r) {
        Product p = id == null ? new Product() : product(id);
        p.name = r.name();
        p.description = r.description();
        p.sku = r.sku();
        p.price = r.price();
        p.stock = r.stock();
        p.status = r.status() == null ? ProductStatus.ACTIVE : r.status();
        p.category = category(r.categoryId());
        p.updatedAt = Instant.now();
        return products.save(p);
    }

    public void deleteProduct(String id) {
        products.delete(product(id));
    }

    public Object products(int page, int limit, String search, String categoryId) {
        List<Product> filtered = products.findAll().stream().filter(p -> (search == null || p.name.toLowerCase().contains(search.toLowerCase())) && (categoryId == null || p.category.id.equals(categoryId))).toList();
        int size = Math.max(1, Math.min(limit, 100));
        int start = Math.min(filtered.size(), Math.max(0, page - 1) * size);
        int end = Math.min(filtered.size(), start + size);
        return Map.of("data", filtered.subList(start, end), "pagination", Map.of("page", page, "limit", size, "totalItems", filtered.size(), "totalPages", (filtered.size() + size - 1) / size));
    }
}
