package com.cloudhis;

import com.cloudhis.mapper.AuthMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:defaultconfig;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
class DefaultConfigurationTest {
    @Autowired AuthMapper users;@Autowired Environment environment;
    @Test void defaultDevelopmentConfigurationBootstrapsUsableAccounts() {
        String password=environment.getRequiredProperty("his.initial-password");
        assertTrue(password.length()>=10);
        assertTrue(new BCryptPasswordEncoder().matches(password,users.byUsername("admin").get("password_hash").toString()));
        assertNotNull(users.byUsername("doctor"));
    }
}
