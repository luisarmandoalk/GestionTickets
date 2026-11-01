package com.campuslands.tickets.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cachés en memoria (Caffeine). El administrador es consciente de las transacciones: una invalidación hecha
 * dentro de una transacción se aplica después del commit, así nadie vuelve a cachear datos sin confirmar.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Métricas de tickets; se invalidan cada vez que un ticket se crea, cambia de estado o se elimina. */
    public static final String METRICAS = "metricasTickets";

    /** Análisis del modelo de lenguaje por título y descripción, para no repetir llamadas al proveedor. */
    public static final String ANALISIS_IA = "analisisIa";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeine = new CaffeineCacheManager();
        caffeine.registerCustomCache(METRICAS, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build());
        caffeine.registerCustomCache(ANALISIS_IA, Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1))
                .build());
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
