package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.auth.AuthResponse;
import com.attila.bookingsystem.dto.auth.LoginRequest;
import com.attila.bookingsystem.dto.auth.RefreshTokenRequest;
import com.attila.bookingsystem.dto.auth.RegisterRequest;
import com.attila.bookingsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // Nem igényel Bearer auth-ot: a kliens access tokenje ilyenkor gyakran már
    // lejárt, csak a refresh token él - a logout ténye a birtokolt refresh
    // tokenből (annak revoke-olásából) fakad, nem egy külön auth-ellenőrzésből.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    // Ezzel szemben ez a végpont a JELENLEGI userhez tartozó ÖSSZES refresh
    // tokent vonja vissza, tehát érvényes access tokent (auth-ot) igényel -
    // lásd SecurityConfig, ahol ez a permitAll("/api/auth/**") alól kivétel.
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.noContent().build();
    }
}
