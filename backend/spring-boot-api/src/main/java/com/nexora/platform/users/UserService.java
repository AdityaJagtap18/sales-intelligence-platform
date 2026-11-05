package com.nexora.platform.users;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.annotation.PostConstruct;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * User Management (/admin/users). This offline Maven build has no Spring
 * Security dependency cached, so there is no HTTP-layer login/session or a
 * filter chain protecting any endpoint (including this one) — see
 * docs/implementation/known-limitations.md. What this does provide is real:
 * persisted users and roles, and credential verification against a salted
 * PBKDF2 hash (never a plaintext or reversibly-encrypted password).
 */
@Service
public class UserService {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    // Sample-app-only default, same pattern as the Postgres/MinIO/Neo4j dev
    // passwords already defaulted in docker-compose.yml/.env.example.
    private static final String DEFAULT_ADMIN_PASSWORD = "nexora_dev_password";

    private final AppUserRepository repository;

    public UserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void seedDefaultAdmin() {
        if (repository.existsByUsernameIgnoreCase(DEFAULT_ADMIN_USERNAME)) {
            return;
        }
        create(new CreateUserRequest(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, "ADMIN"));
    }

    public List<UserDto> list() {
        return repository.findAllByOrderByUsernameAsc().stream().map(UserDto::from).toList();
    }

    public UserDto create(CreateUserRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new DataIntegrityViolationException("Username already exists: " + request.username());
        }
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(request.password(), salt);
        AppUser user = new AppUser(request.username(), hash, salt, request.role(), Instant.now());
        return UserDto.from(repository.save(user));
    }

    public UserDto setRole(long id, String role) {
        AppUser user = find(id);
        user.setRole(role);
        return UserDto.from(repository.save(user));
    }

    public UserDto setEnabled(long id, boolean enabled) {
        AppUser user = find(id);
        user.setEnabled(enabled);
        return UserDto.from(repository.save(user));
    }

    public UserDto resetPassword(long id, String newPassword) {
        AppUser user = find(id);
        String salt = PasswordHasher.newSalt();
        user.setPassword(PasswordHasher.hash(newPassword, salt), salt);
        return UserDto.from(repository.save(user));
    }

    public LoginResponse login(String username, String password) {
        AppUser user = repository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null || !PasswordHasher.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return new LoginResponse(false, "Incorrect username or password.", null);
        }
        if (!user.isEnabled()) {
            return new LoginResponse(false, "This account is disabled.", null);
        }
        user.recordLogin(Instant.now());
        repository.save(user);
        return new LoginResponse(true, "Welcome back, " + user.getUsername() + ".", UserDto.from(user));
    }

    private AppUser find(long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No user with id " + id));
    }
}
