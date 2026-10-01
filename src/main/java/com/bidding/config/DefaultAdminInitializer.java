package com.bidding.config;

import com.bidding.entity.Admin;
import com.bidding.enums.Role;
import com.bidding.repo.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {

        // Auto-migrate dealers table: make email column nullable in MySQL if not already
        try {
            jdbcTemplate.execute("ALTER TABLE dealers MODIFY COLUMN email VARCHAR(255) NULL");
            System.out.println("Dealers table migration: email column successfully set to NULLABLE.");
        } catch (Exception e1) {
            try {
                jdbcTemplate.execute("ALTER TABLE dealers MODIFY email VARCHAR(255) NULL");
                System.out.println("Dealers table migration: email column successfully set to NULLABLE.");
            } catch (Exception ignored) {}
        }

        if (!adminRepository.existsByEmail("admin@gmail.com")) {

            Admin admin = Admin.builder()
                    .fullName("System Admin")
                    .email("admin@gmail.com")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build();

            adminRepository.save(admin);

            System.out.println("==========================================");
            System.out.println(" Default Admin Created Successfully ");
            System.out.println(" Email    : admin@gmail.com");
            System.out.println(" Password : Admin@123");
            System.out.println("==========================================");
        } else {
            Admin admin = adminRepository.findByEmail("admin@gmail.com").get();
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            adminRepository.save(admin);
            System.out.println("==========================================");
            System.out.println(" Default Admin Reset Successfully ");
            System.out.println(" Email    : admin@gmail.com");
            System.out.println(" Password : Admin@123");
            System.out.println("==========================================");
        }
    }
}