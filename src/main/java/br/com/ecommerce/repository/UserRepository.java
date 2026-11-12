package br.com.ecommerce.repository;

import br.com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

import org.springframework.data.domain.*;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
