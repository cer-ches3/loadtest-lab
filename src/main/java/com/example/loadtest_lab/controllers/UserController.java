package com.example.loadtest_lab.controllers;

import com.example.loadtest_lab.entity.User;
import com.example.loadtest_lab.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;
    private final Counter requestCounter;
    private final Timer requestTimer;

    public UserController(UserRepository userRepository, MeterRegistry meterRegistry) {
        this.userRepository = userRepository;

        // Кастомные метрики
        this.requestCounter = Counter.builder("api.users.requests.total")
                .description("Total requests to /api/users")
                .register(meterRegistry);

        this.requestTimer = Timer.builder("api.users.request.duration")
                .description("Request duration to /api/users")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @GetMapping
    public List<User> getAllUser() {
        long start = System.nanoTime();
        requestCounter.increment();

        List<User> users = userRepository.findAll();

        requestTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return users;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        long start = System.nanoTime();
        requestCounter.increment();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User savedUser = userRepository.save(user);

        requestTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<User> deleteUserById(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/slow")
    public String slowResponse() throws InterruptedException {
        Thread.sleep(3000);
        return "Slow response completed";
    }

    @GetMapping("/search")
    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }
}
