package com.traceguard.common.bootstrap;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.UserAccount;
import com.traceguard.auth.UserAccountRepository;
import com.traceguard.auth.UserStatus;
import com.traceguard.tenant.Tenant;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "h2"})
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            TenantRepository tenantRepository,
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tenantRepository.count() > 0) {
            log.info("event=SEED_SKIP reason=TENANTS_EXIST");
            return;
        }

        log.info("event=SEED_START");

        Tenant defaultTenant = new Tenant();
        defaultTenant.setName("Default");
        defaultTenant.setSlug("default");
        defaultTenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(defaultTenant);

        log.info("event=SEED_TENANT tenantId={} slug=default", defaultTenant.getId());

        String adminEmail = System.getenv().getOrDefault("SEED_ADMIN_EMAIL", "admin@traceguard.local");
        String adminPassword = System.getenv().getOrDefault("SEED_ADMIN_PASSWORD", "admin123");

        UserAccount admin = new UserAccount();
        admin.setTenantId(defaultTenant.getId());
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        log.info("event=SEED_ADMIN userId={} email={} role=SUPER_ADMIN", admin.getId(), adminEmail);
        log.info("event=SEED_DONE tenantCount={} userCount={}", tenantRepository.count(), userRepository.count());
    }
}
