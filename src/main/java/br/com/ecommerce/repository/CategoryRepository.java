package br.com.ecommerce.repository;

import br.com.ecommerce.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

import org.springframework.data.domain.*;

public interface CategoryRepository extends JpaRepository<Category, String> {
}
