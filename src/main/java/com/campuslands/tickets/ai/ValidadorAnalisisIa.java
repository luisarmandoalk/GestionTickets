package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.exception.RespuestaIaInvalidaException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Convierte la respuesta cruda del modelo en un análisis confiable: rechaza categorías o prioridades fuera
 * del catálogo y normaliza el resumen (máximo 30 palabras) y las etiquetas (1 a 5, en minúscula, sin repetir).
 */
public class ValidadorAnalisisIa {

    static final int MAX_PALABRAS_RESUMEN = 30;
    static final int MAX_LONGITUD_RESUMEN = 500;
    static final int MAX_ETIQUETAS = 5;
    static final int MAX_LONGITUD_ETIQUETA = 40;

    public AnalisisTicketResponse validar(RespuestaModeloIa respuesta) {
        if (respuesta == null) {
            throw new RespuestaIaInvalidaException("la respuesta está vacía");
        }
        Categoria categoria = convertir(Categoria.class, respuesta.categoria(), "categoria");
        Prioridad prioridad = convertir(Prioridad.class, respuesta.prioridad(), "prioridad");
        return new AnalisisTicketResponse(categoria, prioridad, validarResumen(respuesta.resumen()),
                validarEtiquetas(respuesta.etiquetas()), OrigenAnalisis.MODELO_LENGUAJE);
    }

    /** Acepta variaciones de forma ("crítica", " Alta ") pero no valores fuera del catálogo. */
    private static <E extends Enum<E>> E convertir(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RespuestaIaInvalidaException("falta el campo '%s'".formatted(campo));
        }
        String normalizado = NormalizadorTexto.sinTildes(valor.strip()).toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return Enum.valueOf(tipo, normalizado);
        } catch (IllegalArgumentException e) {
            throw new RespuestaIaInvalidaException("valor no permitido para '%s': '%s'".formatted(campo, valor));
        }
    }

    private static String validarResumen(String resumen) {
        if (resumen == null || resumen.isBlank()) {
            throw new RespuestaIaInvalidaException("falta el campo 'resumen'");
        }
        String limitado = NormalizadorTexto.limitarPalabras(resumen, MAX_PALABRAS_RESUMEN);
        return limitado.length() > MAX_LONGITUD_RESUMEN ? limitado.substring(0, MAX_LONGITUD_RESUMEN) : limitado;
    }

    private static List<String> validarEtiquetas(List<String> etiquetas) {
        List<String> normalizadas = etiquetas == null ? List.of() : etiquetas.stream()
                .filter(Objects::nonNull)
                .map(ValidadorAnalisisIa::normalizarEtiqueta)
                .filter(etiqueta -> !etiqueta.isEmpty())
                .distinct()
                .limit(MAX_ETIQUETAS)
                .toList();
        if (normalizadas.isEmpty()) {
            throw new RespuestaIaInvalidaException("el campo 'etiquetas' debe tener al menos una etiqueta");
        }
        return normalizadas;
    }

    private static String normalizarEtiqueta(String etiqueta) {
        String limpia = NormalizadorTexto.colapsarEspacios(etiqueta.replace("#", "")).toLowerCase(Locale.ROOT);
        return limpia.length() > MAX_LONGITUD_ETIQUETA ? limpia.substring(0, MAX_LONGITUD_ETIQUETA).strip() : limpia;
    }
}
