package br.com.ecommerce.service;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
    }

    public record Register(@NotBlank String name, @Email String email, @Size(min = 6) String password) {
    }

    public record Login(@Email String email, @NotBlank String password) {
    }

    public Object register(Register r) {
        if (users.findByEmail(r.email()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Email já está em uso");
        User u = new User();
        u.name = r.name();
        u.email = r.email();
        u.password = passwords.encode(r.password());
        users.save(u);
        return response(u);
    }

    public Object login(Login r) {
        User u = users.findByEmail(r.email()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        if (!passwords.matches(r.password(), u.password))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        return response(u);
    }

    private Object response(User u) {
        Instant now = Instant.now();
        String access = token(u, now, 900, "access");
        String refresh = token(u, now, 604800, "refresh");
        return Map.of("user", Map.of("id", u.id, "name", u.name, "email", u.email, "role", u.role), "tokens", Map.of("accessToken", access, "refreshToken", refresh));
    }

    private String token(User u, Instant now, long seconds, String type) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ecommerce-springboot").subject(u.id).issuedAt(now).expiresAt(now.plusSeconds(seconds)).claim("role", u.role.name()).claim("type", type).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
