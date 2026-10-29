package com.campuslands.tickets.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoTicketTest {

    @ParameterizedTest
    @CsvSource({
            "ABIERTO, EN_PROCESO, true",
            "EN_PROCESO, RESUELTO, true",
            "RESUELTO, CERRADO, true",
            "RESUELTO, EN_PROCESO, true",
            "ABIERTO, CERRADO, false",
            "ABIERTO, RESUELTO, false",
            "EN_PROCESO, ABIERTO, false",
            "CERRADO, ABIERTO, false",
            "CERRADO, EN_PROCESO, false",
            "ABIERTO, ABIERTO, false"
    })
    void validaLasTransicionesDelCicloDeVida(EstadoTicket origen, EstadoTicket destino, boolean permitida) {
        assertThat(origen.permiteTransicionA(destino)).isEqualTo(permitida);
    }
}
