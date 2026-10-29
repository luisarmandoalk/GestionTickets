package com.campuslands.tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.exception.ProveedorIaNoDisponibleException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalizadorConRespaldoTest {

    private static final String TITULO = "Sin internet";
    private static final String DESCRIPCION = "No tengo internet desde esta mañana en la oficina.";

    private final AnalizadorTicketService principal = mock(AnalizadorTicketService.class);
    private final AnalizadorTicketService respaldo = mock(AnalizadorTicketService.class);
    private final AnalizadorConRespaldo analizador = new AnalizadorConRespaldo(principal, respaldo);

    @Test
    void usaElAnalizadorPrincipalCuandoResponde() {
        AnalisisTicketResponse delModelo = analisis(OrigenAnalisis.MODELO_LENGUAJE);
        when(principal.analizar(TITULO, DESCRIPCION)).thenReturn(delModelo);

        assertThat(analizador.analizar(TITULO, DESCRIPCION)).isEqualTo(delModelo);
        verifyNoInteractions(respaldo);
    }

    @Test
    void recurreAlRespaldoCuandoElPrincipalFalla() {
        when(principal.analizar(TITULO, DESCRIPCION))
                .thenThrow(new ProveedorIaNoDisponibleException("sin conexión", true, null));
        when(respaldo.analizar(TITULO, DESCRIPCION)).thenReturn(analisis(OrigenAnalisis.REGLAS));

        assertThat(analizador.analizar(TITULO, DESCRIPCION).origen()).isEqualTo(OrigenAnalisis.REGLAS);
    }

    private static AnalisisTicketResponse analisis(OrigenAnalisis origen) {
        return new AnalisisTicketResponse(Categoria.CONECTIVIDAD, Prioridad.MEDIA, "Sin internet.",
                List.of("internet"), origen);
    }
}
