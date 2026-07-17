package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.AppRole;
import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.dto.auth.AuthResponse;
import com.attila.bookingsystem.dto.auth.LoginRequest;
import com.attila.bookingsystem.dto.auth.RefreshTokenRequest;
import com.attila.bookingsystem.dto.auth.RegisterRequest;
import com.attila.bookingsystem.exception.EmailAlreadyInUseException;
import com.attila.bookingsystem.repository.AppRoleRepository;
import com.attila.bookingsystem.repository.AppUserRepository;
import com.attila.bookingsystem.security.CurrentUserProvider;
import com.attila.bookingsystem.security.JwtService;
import com.attila.bookingsystem.config.JwtProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_GUEST";

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserProvider currentUserProvider;

    public AuthService(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository,
                        PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                        JwtService jwtService, JwtProperties jwtProperties, RefreshTokenService refreshTokenService,
                        CurrentUserProvider currentUserProvider) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        AppUser user = new AppUser(request.email(), passwordEncoder.encode(request.password()), request.fullName());

        // Minden regisztráló user alapból ROLE_GUEST-et kap - a ROLE_PROVIDER csak
        // admin jóváhagyás után kerül hozzá (lásd Provider entitás), ROLE_ADMIN
        // pedig csak manuálisan/seeddel adható, itt sosem.
        AppRole guestRole = appRoleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE + " role missing - check V1 migration seed data"));
        user.setRoles(Set.of(guestRole));

        appUserRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Ha rossz az email/jelszó, ez BadCredentialsException-t dob, amit a
        // GlobalExceptionHandler egységes, user-enumeration-mentes 401-re alakít.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User authenticated but not found: " + request.email()));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(request.refreshToken());
        return buildAuthResponse(result.user(), result.rawToken());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional
    public void logoutAll() {
        AppUser currentUser = currentUserProvider.getCurrentUser();
        refreshTokenService.revokeAllForUser(currentUser.getId());
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        return buildAuthResponse(user, refreshTokenService.issue(user));
    }

    private AuthResponse buildAuthResponse(AppUser user, String refreshToken) {
        List<String> roleNames = user.getRoles().stream().map(AppRole::getName).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenExpirationMs(),
                user.getEmail(),
                user.getFullName(),
                roleNames
        );
    }
}
