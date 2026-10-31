package com.carehub.carehub.controller;

import com.carehub.carehub.entity.User;
import com.carehub.carehub.entity.Patient;
import com.carehub.carehub.repository.UserRepository;
import com.carehub.carehub.repository.PatientRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Session-based authentication. On successful login the SecurityContext is
 * stored in the HTTP session (via HttpSessionSecurityContextRepository) so
 * every subsequent request on the same session cookie is authenticated —
 * no token handling needed on the frontend.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private com.carehub.carehub.service.AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
            auditLogService.log("LOGIN", "Auth", "User '" + username + "' signed in successfully (Role: " + user.getRole() + ")", String.valueOf(user.getUserId()));
            return ResponseEntity.ok(toUserInfo(user));
        } catch (DisabledException ex) {
            return errorResponse(HttpStatus.FORBIDDEN, "This account has been deactivated. Contact an administrator.");
        } catch (BadCredentialsException ex) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        auditLogService.log("LOGOUT", "Auth", "User logged out", null);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Not signed in");
        }
        
        // Check if patient
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            Optional<Patient> patient = patientRepository.findByPhone(auth.getName());
            return patient.<ResponseEntity<?>>map(p -> ResponseEntity.ok(toPatientInfo(p)))
                    .orElseGet(() -> errorResponse(HttpStatus.UNAUTHORIZED, "Not signed in"));
        }

        Optional<User> user = userRepository.findByUsernameIgnoreCase(auth.getName());
        return user.<ResponseEntity<?>>map(u -> ResponseEntity.ok(toUserInfo(u)))
                .orElseGet(() -> errorResponse(HttpStatus.UNAUTHORIZED, "Not signed in"));
    }

    @PostMapping("/patient-login")
    public ResponseEntity<?> patientLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String phone = body.get("phone");
        String dobString = body.get("dob"); // Expected format: YYYY-MM-DD

        if (phone == null || phone.isBlank() || dobString == null || dobString.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Phone number and Date of Birth are required");
        }

        try {
            LocalDate dob = LocalDate.parse(dobString);
            Optional<Patient> patientOpt = patientRepository.findByPhone(phone);

            if (patientOpt.isEmpty() || !patientOpt.get().getDateOfBirth().equals(dob)) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "Invalid phone number or Date of Birth");
            }

            Patient patient = patientOpt.get();

            // Create authentication with ROLE_PATIENT
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    patient.getPhone(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PATIENT")));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            return ResponseEntity.ok(toPatientInfo(patient));
        } catch (Exception ex) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Invalid date format. Expected YYYY-MM-DD.");
        }
    }

    private Map<String, Object> toPatientInfo(Patient patient) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("patientId", patient.getPatientId());
        resp.put("username", patient.getPhone());
        resp.put("role", "Patient");
        resp.put("name", patient.getName());
        return resp;
    }

    /**
     * Self-registration is left in for demo convenience but only ever
     * creates the FIRST user (bootstrap Admin) — after that, staff accounts
     * must be created by an Admin via /api/users so role assignment can't
     * be self-granted.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        if (userRepository.count() > 0) {
            return errorResponse(HttpStatus.FORBIDDEN,
                    "Self-registration is disabled. Ask an Admin to create your account from Users management.");
        }
        String username = body.get("username");
        String password = body.get("password");
        String role = body.getOrDefault("role", "Admin");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .build();
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toUserInfo(saved));
    }

    private Map<String, Object> toUserInfo(User user) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", user.getUserId());
        resp.put("username", user.getUsername());
        resp.put("role", user.getRole());
        return resp;
    }

    private ResponseEntity<?> errorResponse(HttpStatus status, String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("message", message);
        return ResponseEntity.status(status).body(err);
    }
}
