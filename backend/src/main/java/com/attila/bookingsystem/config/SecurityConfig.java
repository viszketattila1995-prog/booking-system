package com.attila.bookingsystem.config;

import com.attila.bookingsystem.security.AppUserDetailsService;
import com.attila.bookingsystem.security.JwtAuthenticationFilter;
import com.attila.bookingsystem.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final JwtService jwtService;

    public SecurityConfig(AppUserDetailsService appUserDetailsService, JwtService jwtService) {
        this.appUserDetailsService = appUserDetailsService;
        this.jwtService = jwtService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF a klasszikus "sütiben tárolt session" ellen véd (a böngésző
                // automatikusan elküldi a sütit egy idegen oldalról indított kérésnél is).
                // Nálunk nincs session-süti: a JWT-t a kliens explicit teszi be az
                // Authorization fejlécbe minden kérésnél, ezt egy idegen oldal nem tudja
                // "véletlenül" kiváltani (nincs ambiens hitelesítő adat, amit a böngésző
                // automatikusan csatolna). Ezért CSRF védelem stateless JWT mellett
                // felesleges rétegnek számít itt, kikapcsoljuk.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Nincs szerver-oldali session: minden kérés önmagában hitelesített
                // a JWT alapján, a szerver nem tárol authentikációs állapotot kérések között.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Explicit whitelist (nem "*"), mert a JWT-t az Authorization fejlécben küldjük,
        // és böngészőben a fejléces kéréseknél a "*" origin + hitelesítő adat kombináció
        // amúgy sem engedélyezett - de konkrét, ismert origin-t whitelistelni akkor is jó
        // gyakorlat, ha épp nem küldenénk cookie-t, mert korlátozza, mely oldalak
        // hívhatják egyáltalán az API-t a böngészőből.
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
