package br.com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;

import java.time.Instant;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public UserService(UserRepository u, PasswordEncoder p) {
        users = u;
        passwords = p;
    }

    public record UpdateUser(String name, String email, String password) {
    }

    public User get(String id, String requester, String role) {
        check(id, requester, role);
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public User update(String id, UpdateUser r, String requester, String role) {
        User u = get(id, requester, role);
        if (r.name() != null) u.name = r.name();
        if (r.email() != null) u.email = r.email();
        if (r.password() != null && !r.password().isBlank()) u.password = passwords.encode(r.password());
        u.updatedAt = Instant.now();
        return users.save(u);
    }

    public void delete(String id, String requester, String role) {
        users.delete(get(id, requester, role));
    }

    private void check(String id, String requester, String role) {
        if (!id.equals(requester) && !"ADMIN".equals(role))
            throw new ApiException(HttpStatus.FORBIDDEN, "Acesso negado");
    }
}
