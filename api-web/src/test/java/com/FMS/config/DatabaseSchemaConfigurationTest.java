package com.FMS.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseSchemaConfigurationTest {

    @Test
    void flywayOwnsSchemaChangesAndHibernateOnlyValidates() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"));
        PropertySource<?> application = sources.getFirst();

        assertEquals("validate", application.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals(Boolean.FALSE, application.getProperty("spring.jpa.generate-ddl"));
        assertEquals(Boolean.FALSE, application.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals(Boolean.TRUE, application.getProperty("spring.flyway.validate-on-migrate"));
        assertEquals(Boolean.TRUE, application.getProperty("spring.flyway.validate-migration-naming"));
        assertEquals(Boolean.TRUE, application.getProperty("spring.flyway.clean-disabled"));
    }
}
