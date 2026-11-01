package com.campuslands.tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;

class AnalizadorConCacheTest {

    private static final String TITULO = "Problema con internet";
    private static final String DESCRIPCION = "Desde esta mañana no puedo conectarme a internet.";

    private final AnalizadorTicketService analizador = mock(AnalizadorTicketService.class);
    private final AnalizadorConCache conCache = new AnalizadorConCache(analizador, new ConcurrentMapCache("prueba"));

    @Test
    void reutilizaElAnalisisDelModeloParaElMismoTicket() {
        when(analizador.analizar(TITULO, DESCRIPCION)).thenReturn(analisis(OrigenAnalisis.MODELO_LENGUAJE));

        AnalisisTicketResponse primero = conCache.analizar(TITULO, DESCRIPCION);
        AnalisisTicketResponse segundo = conCache.analizar("  " + TITULO, DESCRIPCION + " ");

        assertThat(segundo).isEqualTo(primero);
        verify(analizador, times(1)).analizar(TITULO, DESCRIPCION);
    }

    @Test
    void noGuardaElResultadoDelRespaldoParaVolverAIntentarConElModelo() {
        when(analizador.analizar(TITULO, DESCRIPCION))
                .thenReturn(analisis(OrigenAnalisis.REGLAS), analisis(OrigenAnalisis.MODELO_LENGUAJE));

        assertThat(conCache.analizar(TITULO, DESCRIPCION).origen()).isEqualTo(OrigenAnalisis.REGLAS);
        assertThat(conCache.analizar(TITULO, DESCRIPCION).origen()).isEqualTo(OrigenAnalisis.MODELO_LENGUAJE);
        verify(analizador, times(2)).analizar(TITULO, DESCRIPCION);
    }

    private static AnalisisTicketResponse analisis(OrigenAnalisis origen) {
        return new AnalisisTicketResponse(Categoria.CONECTIVIDAD, Prioridad.ALTA, "Usuario sin internet.",
                List.of("internet"), origen);
    }
}
