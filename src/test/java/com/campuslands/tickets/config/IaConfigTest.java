package com.campuslands.tickets.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslands.tickets.ai.AnalisisTicketResponse;
import com.campuslands.tickets.ai.AnalizadorConRespaldo;
import com.campuslands.tickets.ai.AnalizadorLlmService;
import com.campuslands.tickets.ai.AnalizadorReglasService;
import com.campuslands.tickets.ai.AnalizadorTicketService;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IaConfigTest {

    /** Puerto sin servicio: cualquier llamada al proveedor falla por conexión rechazada o tiempo límite. */
    private static final String PROVEEDOR_INALCANZABLE = "http://127.0.0.1:9/v1";

    private final IaConfig configuracion = new IaConfig();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void enModoAutoSinApiKeyUsaElClasificadorPorReglas() {
        assertThat(configuracion.analizadorTicketService(propiedades(ModoIa.AUTO, "", true, "modelo"), jsonMapper))
                .isInstanceOf(AnalizadorReglasService.class);
    }

    @Test
    void enModoAutoConApiKeyUsaElModeloConRespaldo() {
        assertThat(configuracion.analizadorTicketService(propiedades(ModoIa.AUTO, "clave", true, "modelo"), jsonMapper))
                .isInstanceOf(AnalizadorConRespaldo.class);
    }

    @Test
    void sinRespaldoUsaSoloElModelo() {
        assertThat(configuracion.analizadorTicketService(propiedades(ModoIa.LLM, "", false, "modelo"), jsonMapper))
                .isInstanceOf(AnalizadorLlmService.class);
    }

    @Test
    void exigeElNombreDelModeloCuandoSeUsaElModeloDeLenguaje() {
        assertThatThrownBy(() -> configuracion.analizadorTicketService(propiedades(ModoIa.LLM, "", true, " "), jsonMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_MODEL");
    }

    @Test
    void siElProveedorNoRespondeElTicketSeClasificaConReglas() {
        AnalizadorTicketService analizador =
                configuracion.analizadorTicketService(propiedades(ModoIa.LLM, "", true, "modelo"), jsonMapper);

        AnalisisTicketResponse analisis = analizador.analizar("Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.");

        assertThat(analisis.origen()).isEqualTo(OrigenAnalisis.REGLAS);
        assertThat(analisis.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
    }

    private static IaProperties propiedades(ModoIa modo, String apiKey, boolean respaldo, String modelo) {
        return new IaProperties(modo, respaldo, PROVEEDOR_INALCANZABLE, apiKey, modelo, Duration.ofSeconds(2), 0,
                Duration.ZERO, true);
    }
}
