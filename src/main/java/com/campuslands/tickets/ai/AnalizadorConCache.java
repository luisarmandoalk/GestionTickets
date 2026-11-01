package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.OrigenAnalisis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

/**
 * Reutiliza el análisis del modelo para tickets con el mismo título y descripción. Solo guarda resultados del
 * modelo: si el análisis vino del respaldo por reglas, la próxima vez se vuelve a intentar con el modelo.
 */
public class AnalizadorConCache implements AnalizadorTicketService {

    private static final Logger log = LoggerFactory.getLogger(AnalizadorConCache.class);

    private final AnalizadorTicketService analizador;
    private final Cache cache;

    public AnalizadorConCache(AnalizadorTicketService analizador, Cache cache) {
        this.analizador = analizador;
        this.cache = cache;
    }

    @Override
    public AnalisisTicketResponse analizar(String titulo, String descripcion) {
        ClaveAnalisis clave = new ClaveAnalisis(titulo.strip(), descripcion.strip());
        AnalisisTicketResponse guardado = cache.get(clave, AnalisisTicketResponse.class);
        if (guardado != null) {
            log.debug("Análisis tomado de la caché para el ticket '{}'", clave.titulo());
            return guardado;
        }
        AnalisisTicketResponse analisis = analizador.analizar(titulo, descripcion);
        if (analisis.origen() == OrigenAnalisis.MODELO_LENGUAJE) {
            cache.put(clave, analisis);
        }
        return analisis;
    }

    private record ClaveAnalisis(String titulo, String descripcion) {
    }
}
