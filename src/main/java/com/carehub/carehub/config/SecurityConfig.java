package com.carehub.carehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security configuration.
 *
 * Authentication: session-based. The frontend POSTs JSON credentials to
 * /api/auth/login (see AuthController), which authenticates against
 * {@link CustomUserDetailsService} (BCrypt-hashed passwords) and stores the
 * resulting SecurityContext in the HTTP session — the browser then just
 * carries the session cookie on every subsequent request.
 *
 * Authorization: each module is restricted to the roles described in the
 * CareHub role matrix (Admin / Doctor / Receptionist / Pharmacist).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

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
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("message", message);
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1).maxSessionsPreventsLogin(false))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeJsonError(res, HttpStatus.UNAUTHORIZED.value(), "Please sign in to continue."))
                .accessDeniedHandler((req, res, ex) -> writeJsonError(res, HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this action.")))
            .authorizeHttpRequests(auth -> auth
                // Public: static assets and login/logout endpoints
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // Dashboard + AI + alerts: every signed-in role, module-level filtering happens in the UI
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").authenticated()

                // Users / staff accounts: Admin only
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Reports: Admin only ("View all reports" is an Admin-only permission)
                .requestMatchers("/api/reports/**").hasRole("ADMIN")

                // Notifications: every authenticated user
                .requestMatchers("/api/notifications/**").authenticated()

                // Audit logs: Admin only
                .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")

                // Prescription OCR: Admin and Doctor
                .requestMatchers("/api/ai/prescription-ocr/**").hasAnyRole("ADMIN", "DOCTOR")

                // AI module (predictions, reorder suggestions, fast/slow movers, insights): Admin, Pharmacist, Doctor
                .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "PHARMACIST", "DOCTOR")

                // Appointments: Admin, Doctor, Receptionist
                .requestMatchers(HttpMethod.GET, "/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "PATIENT")
                .requestMatchers(HttpMethod.POST, "/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT, "/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PATCH, "/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST")

                // Medical History: Admin and Doctor manage; Patient can view
                .requestMatchers(HttpMethod.GET, "/api/patients/*/history").hasAnyRole("ADMIN", "DOCTOR", "PATIENT")
                .requestMatchers(HttpMethod.POST, "/api/patients/*/history").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/medical-history/**").hasAnyRole("ADMIN", "DOCTOR")

                // Purchase orders: Admin approves/rejects, Pharmacist creates/views
                .requestMatchers(HttpMethod.PATCH, "/api/purchase-orders/*/approve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/purchase-orders/*/reject").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/purchase-orders/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.POST, "/api/purchase-orders/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PATCH, "/api/purchase-orders/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/purchase-orders/**").hasRole("ADMIN")

                // Suppliers: Admin + Pharmacist
                .requestMatchers(HttpMethod.GET, "/api/suppliers/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.POST, "/api/suppliers/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT, "/api/suppliers/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**").hasRole("ADMIN")

                // Medicines: Admin + Pharmacist manage, Doctor can view
                .requestMatchers(HttpMethod.GET, "/api/medicines/**").hasAnyRole("ADMIN", "PHARMACIST", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/medicines/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT, "/api/medicines/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/medicines/**").hasRole("ADMIN")

                // Inventory: Admin + Pharmacist only
                .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "PHARMACIST")

                // Prescriptions: Admin + Doctor manage, Pharmacist can view (to dispense), Patient can view their own
                .requestMatchers(HttpMethod.GET, "/api/prescriptions/by-patient/*").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                .requestMatchers(HttpMethod.GET, "/api/prescription-items/by-prescription/*").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/prescription-items/**").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/prescription-items/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/api/prescription-items/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/prescription-items/**").hasRole("ADMIN")

                // Billing: Admin + Receptionist only, Patient can view their own
                .requestMatchers(HttpMethod.GET, "/api/bills/by-patient/*").hasAnyRole("ADMIN", "RECEPTIONIST", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/bill-items/by-bill/*").hasAnyRole("ADMIN", "RECEPTIONIST", "PATIENT")
                .requestMatchers("/api/bills/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/api/bill-items/**").hasAnyRole("ADMIN", "RECEPTIONIST")

                // Patients: Admin, Doctor, Receptionist can view; Admin + Receptionist can create/edit; Admin deletes; Patient views their own
                .requestMatchers(HttpMethod.GET, "/api/patients/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/patients/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasRole("ADMIN")

                // Doctors module: Admin, Doctor, Receptionist can view; only Admin manages
                .requestMatchers(HttpMethod.GET, "/api/doctors/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/api/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/doctors/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
