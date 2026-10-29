package com.campuslands.tickets;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.springframework.boot.SpringApplication;

/**
 * Ejecuta la aplicación con un PostgreSQL embebido, sin instalar PostgreSQL ni Docker:
 * {@code ./mvnw spring-boot:test-run}. Los datos se pierden al detener la aplicación.
 */
public class GestionTicketsIaConPostgresEmbebido {

    public static void main(String[] args) throws IOException {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> detener(postgres)));
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        SpringApplication.run(GestionTicketsIaApplication.class, args);
    }

    private static void detener(EmbeddedPostgres postgres) {
        try {
            postgres.close();
        } catch (IOException e) {
            System.err.println("No fue posible detener PostgreSQL embebido: " + e.getMessage());
        }
    }
}
