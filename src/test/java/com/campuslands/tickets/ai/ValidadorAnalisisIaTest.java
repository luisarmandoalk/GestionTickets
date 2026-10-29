package com.campuslands.tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.exception.RespuestaIaInvalidaException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidadorAnalisisIaTest {

    private final ValidadorAnalisisIa validador = new ValidadorAnalisisIa();

    @Test
    void normalizaValoresConTildesEspaciosYMayusculas() {
        AnalisisTicketResponse analisis = validador.validar(new RespuestaModeloIa(" hardware ", "Crítica",
                "  Servidor   sin respuesta.  ", List.of("#Servidor", "servidor", " Caída  Total ", "")));

        assertThat(analisis.categoria()).isEqualTo(Categoria.HARDWARE);
        assertThat(analisis.prioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(analisis.resumen()).isEqualTo("Servidor sin respuesta.");
        assertThat(analisis.etiquetas()).containsExactly("servidor", "caída total");
        assertThat(analisis.origen()).isEqualTo(OrigenAnalisis.MODELO_LENGUAJE);
    }

    @Test
    void recortaElResumenYLasEtiquetasALosLimites() {
        String resumenLargo = "palabra ".repeat(45);
        List<String> etiquetas = List.of("a", "b", "c", "d", "e", "f", "g");

        AnalisisTicketResponse analisis = validador.validar(
                new RespuestaModeloIa("OTRO", "BAJA", resumenLargo, etiquetas));

        assertThat(analisis.resumen().split(" ")).hasSize(30);
        assertThat(analisis.etiquetas()).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void rechazaUnaCategoriaFueraDelCatalogo() {
        RespuestaModeloIa respuesta = new RespuestaModeloIa("REDES", "ALTA", "Resumen.", List.of("red"));

        assertThatThrownBy(() -> validador.validar(respuesta))
                .isInstanceOf(RespuestaIaInvalidaException.class)
                .hasMessageContaining("categoria")
                .hasMessageContaining("REDES");
    }

    @Test
    void rechazaUnaPrioridadFueraDelCatalogo() {
        RespuestaModeloIa respuesta = new RespuestaModeloIa("SOFTWARE", "URGENTISIMA", "Resumen.", List.of("app"));

        assertThatThrownBy(() -> validador.validar(respuesta))
                .isInstanceOf(RespuestaIaInvalidaException.class)
                .hasMessageContaining("prioridad");
    }

    @Test
    void rechazaUnaRespuestaSinEtiquetasNiResumen() {
        assertThatThrownBy(() -> validador.validar(new RespuestaModeloIa("OTRO", "BAJA", "Resumen.", List.of(" "))))
                .isInstanceOf(RespuestaIaInvalidaException.class)
                .hasMessageContaining("etiquetas");
        assertThatThrownBy(() -> validador.validar(new RespuestaModeloIa("OTRO", "BAJA", null, List.of("x"))))
                .isInstanceOf(RespuestaIaInvalidaException.class)
                .hasMessageContaining("resumen");
    }
}
