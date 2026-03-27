package com.example.loadtest_lab.services;

import com.example.loadtest_lab.entity.User;
import com.example.loadtest_lab.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Counter requestCounter;
    private final Timer requestTimer;

    public UserService(UserRepository userRepository, MeterRegistry meterRegistry) {
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

    public List<User> getAllUser() {
        long start = System.nanoTime();
        requestCounter.increment();

        List<User> users = userRepository.findAll();

        requestTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return users;
    }

    public ResponseEntity<User> getUserById(Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<User> createUser(User user) {
        long start = System.nanoTime();
        requestCounter.increment();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User savedUser = userRepository.save(user);

        requestTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    public ResponseEntity<User> deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public String slowResponse() throws InterruptedException {
        Thread.sleep(3000);
        return "Slow response completed";
    }

    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }
}
