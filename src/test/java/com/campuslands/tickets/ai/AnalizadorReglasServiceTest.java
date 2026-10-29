package com.campuslands.tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnalizadorReglasServiceTest {

    private final AnalizadorReglasService analizador = new AnalizadorReglasService();

    @Test
    void clasificaElEjemploDelEnunciadoComoConectividadDePrioridadAlta() {
        AnalisisTicketResponse analisis = analizador.analizar("Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.");

        assertThat(analisis.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(analisis.prioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(analisis.etiquetas()).contains("conectividad", "internet", "reunión");
        assertThat(analisis.resumen()).isEqualTo(
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.");
        assertThat(analisis.origen()).isEqualTo(OrigenAnalisis.REGLAS);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Clave bloqueada        | Olvidé mi contraseña y la cuenta quedó bloqueada en el portal    | ACCESOS
            Equipo dañado          | La pantalla del computador no enciende desde que llegué hoy      | HARDWARE
            Falla en facturación   | La aplicación de facturación muestra un error al guardar         | SOFTWARE
            Sin wifi               | El WiFi del piso tres se cae cada cinco minutos durante el día   | CONECTIVIDAD
            Solicitud de silla     | Necesito una silla nueva para el puesto de trabajo de la sede    | OTRO
            """)
    void clasificaLaCategoriaSegunLasPalabrasClave(String titulo, String descripcion, Categoria esperada) {
        assertThat(analizador.analizar(titulo, descripcion).categoria()).isEqualTo(esperada);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Producción detenida, ningún pedido se puede facturar en la planta              | CRITICA
            Todos los usuarios de la sede perdieron acceso al sistema contable              | CRITICA
            Es urgente, no puedo trabajar sin acceso a la carpeta compartida               | ALTA
            Quisiera cambiar el fondo de pantalla, no es urgente, cuando puedan lo revisan  | BAJA
            Tengo una consulta sobre cómo configurar la firma del correo                    | BAJA
            La impresora imprime las hojas con manchas grises en el borde                   | MEDIA
            """)
    void asignaLaPrioridadSegunElTexto(String descripcion, Prioridad esperada) {
        assertThat(analizador.analizar("Solicitud de soporte", descripcion).prioridad()).isEqualTo(esperada);
    }

    @Test
    void reconocePalabrasCompletasYNoFragmentos() {
        AnalisisTicketResponse analisis = analizador.analizar("Credenciales del portal",
                "Necesito restablecer mis credenciales del portal de proveedores.");

        assertThat(analisis.categoria()).isEqualTo(Categoria.ACCESOS);
        assertThat(analisis.etiquetas()).doesNotContain("red");
    }

    @Test
    void textoSinPalabrasClaveQuedaComoOtroConPrioridadMediaYUnaEtiqueta() {
        AnalisisTicketResponse analisis = analizador.analizar("Solicitud general",
                "Quiero solicitar el préstamo de una silla para la sala.");

        assertThat(analisis.categoria()).isEqualTo(Categoria.OTRO);
        assertThat(analisis.prioridad()).isEqualTo(Prioridad.MEDIA);
        assertThat(analisis.etiquetas()).containsExactly("general");
    }

    @Test
    void generaEntreUnaYCincoEtiquetasSinRepetir() {
        AnalisisTicketResponse analisis = analizador.analizar("Internet, wifi, VPN y router",
                "No hay internet ni wifi, la VPN no conecta, el router parpadea y es urgente para la reunión con el cliente.");

        assertThat(analisis.etiquetas()).hasSizeBetween(1, 5).doesNotHaveDuplicates();
    }

    @Test
    void limitaElResumenATreintaPalabras() {
        String descripcion = "palabra ".repeat(60).trim();

        String resumen = analizador.analizar("Texto muy largo", descripcion).resumen();

        assertThat(resumen.split(" ")).hasSize(30);
        assertThat(resumen).endsWith("...");
    }
}
