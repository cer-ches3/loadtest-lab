package com.example.loadtest_lab;

import com.example.loadtest_lab.entity.User;
import com.example.loadtest_lab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePopulator implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Starting database population (BATCH DEMO)");
        log.info("========================================");

        // Сначала очистим таблицу
        userRepository.deleteAll();
        log.info("Cleared existing users");

        // ДЕМОНСТРАЦИЯ: вставка с разными подходами
        // Для реальной нагрузки раскомментируйте нужный метод

        // testSingleInsert(1000);      // очень медленно, не использовать
        testBatchInsert(100000);        // быстро
        //testJpaBatchInsert(100000);

        log.info("========================================");
        log.info("Database population completed");
        log.info("========================================");
    }

    private void testSingleInsert(int count) {
        log.info("=== Single insert (JDBC) ===");
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)",
                    "User " + i,
                    "user" + i + "@test.com",
                    LocalDateTime.now()
            );
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Inserted {} records in {} ms ({} records/sec)",
                count, duration, (count * 1000 / duration));
    }

    private void testBatchInsert(int count) {
        log.info("=== Batch insert (JDBC with batch) ===");
        long start = System.currentTimeMillis();

        String sql = "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)";

        int batchSize = 1000;
        List<Object[]> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            batch.add(new Object[]{
                    "User " + i,
                    "user" + i + "@test.com",
                    LocalDateTime.now()
            });

            if (batch.size() >= batchSize) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
                if (i % 10000 == 0) {
                    log.info("Inserted {} records", i + 1);
                }
            }
        }

        // Остаток
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Inserted {} records in {} ms ({} records/sec)",
                count, duration, (count * 1000 / duration));
    }

    private void testJpaBatchInsert(int count) {
        log.info("=== Batch insert (JPA with flush) ===");
        long start = System.currentTimeMillis();

        int batchSize = 1000;
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setName("User " + i);
            user.setEmail("user" + i + "@test.com");
            user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);

            if (i % batchSize == 0 && i > 0) {
                userRepository.flush();
                userRepository.deleteAll();
                log.info("Flushed {} records", i);
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Inserted {} records in {} ms ({} records/sec)",
                count, duration, (count * 1000 / duration));
    }
}
