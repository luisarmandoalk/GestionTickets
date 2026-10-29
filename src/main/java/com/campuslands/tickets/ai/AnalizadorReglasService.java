package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clasificador local basado en palabras clave. No depende de servicios externos, por lo que sirve
 * como analizador principal cuando no hay API key y como respaldo cuando el modelo de lenguaje falla.
 *
 * <p>Categoría: gana la que acumule más peso de palabras clave (en empate, la primera de la lista).
 * Prioridad: se aplica la primera regla con alguna coincidencia; si ninguna coincide, MEDIA.
 */
public class AnalizadorReglasService implements AnalizadorTicketService {

    private static final int MAX_PALABRAS_RESUMEN = 30;
    private static final int MIN_PALABRAS_PRIMERA_ORACION = 4;
    private static final int MAX_ETIQUETAS = 5;
    private static final String ETIQUETA_SIN_CATEGORIA = "general";
    private static final Pattern FIN_DE_ORACION = Pattern.compile("[.!?](\\s|$)");

    private static final String INICIO_PALABRA = "(?<![\\p{L}\\p{N}])";
    private static final String FIN_PALABRA = "(?![\\p{L}\\p{N}])";

    private static final List<ReglaCategoria> REGLAS_CATEGORIA = List.of(
            new ReglaCategoria(Categoria.CONECTIVIDAD, "conectividad", List.of(
                    clave("internet", 3, "internet"),
                    clave("wifi", 3, "wifi"),
                    clave("wi-fi", 3, "wifi"),
                    clave("vpn", 3, "vpn"),
                    clave("router", 3, "router"),
                    clave("red", 2, "red"),
                    clave("ethernet", 2, "red"),
                    clave("conect*", 2, "conexión"),
                    clave("desconect*", 2, "conexión"),
                    clave("conexion*", 2, "conexión"),
                    clave("senal", 2, "señal"),
                    clave("proxy", 2, "proxy"),
                    clave("dns", 2, "dns"))),
            new ReglaCategoria(Categoria.ACCESOS, "accesos", List.of(
                    clave("contrasena*", 3, "contraseña"),
                    clave("password", 3, "contraseña"),
                    clave("clave", 2, "contraseña"),
                    clave("login", 3, "inicio de sesión"),
                    clave("iniciar sesion", 3, "inicio de sesión"),
                    clave("inicio de sesion", 3, "inicio de sesión"),
                    clave("credencial*", 3, "credenciales"),
                    clave("autenticacion", 3, "autenticación"),
                    clave("permiso*", 3, "permisos"),
                    clave("acceso*", 2, "acceso"),
                    clave("acceder", 2, "acceso"),
                    clave("bloquead*", 2, "cuenta bloqueada"),
                    clave("cuenta", 1, "cuenta"))),
            new ReglaCategoria(Categoria.HARDWARE, "hardware", List.of(
                    clave("pantalla*", 3, "pantalla"),
                    clave("monitor*", 3, "monitor"),
                    clave("teclado*", 3, "teclado"),
                    clave("mouse", 3, "mouse"),
                    clave("raton", 3, "mouse"),
                    clave("impresora*", 3, "impresora"),
                    clave("disco duro", 3, "disco"),
                    clave("bateria", 3, "batería"),
                    clave("cargador", 3, "cargador"),
                    clave("no enciende", 3, "encendido"),
                    clave("camara", 2, "cámara"),
                    clave("diadema*", 2, "diadema"),
                    clave("computador*", 2, "computador"),
                    clave("portatil*", 2, "portátil"),
                    clave("equipo", 1, "equipo"))),
            new ReglaCategoria(Categoria.SOFTWARE, "software", List.of(
                    clave("software", 3, "software"),
                    clave("sistema operativo", 3, "sistema operativo"),
                    clave("excel", 3, "office"),
                    clave("word", 3, "office"),
                    clave("outlook", 3, "office"),
                    clave("office", 3, "office"),
                    clave("windows", 2, "windows"),
                    clave("navegador", 2, "navegador"),
                    clave("programa*", 2, "programa"),
                    clave("aplicacion*", 2, "aplicación"),
                    clave("app", 2, "aplicación"),
                    clave("instal*", 2, "instalación"),
                    clave("actualiza*", 2, "actualización"),
                    clave("licencia*", 2, "licencia"),
                    clave("se cierra", 2, "cierre inesperado"),
                    clave("se congela", 2, "congelamiento"),
                    clave("error*", 1, "error"))));

    /** Se evalúan en orden: las solicitudes explícitamente no urgentes se reconocen antes que las urgentes. */
    private static final List<ReglaPrioridad> REGLAS_PRIORIDAD = List.of(
            new ReglaPrioridad(Prioridad.CRITICA, List.of(
                    clave("produccion detenida", 1, "producción"),
                    clave("produccion caida", 1, "producción"),
                    clave("produccion parada", 1, "producción"),
                    clave("todos los usuarios", 1, "afectación masiva"),
                    clave("toda la empresa", 1, "afectación masiva"),
                    clave("toda la oficina", 1, "afectación masiva"),
                    clave("nadie puede", 1, "afectación masiva"),
                    clave("servidor caido", 1, "servidor"),
                    clave("caida total", 1, "caída total"),
                    clave("perdida de datos", 1, "pérdida de datos"),
                    clave("virus", 1, "seguridad"),
                    clave("ransomware", 1, "seguridad"),
                    clave("hackea*", 1, "seguridad"),
                    clave("brecha de seguridad", 1, "seguridad"),
                    clave("fuga de informacion", 1, "seguridad"),
                    clave("critic*", 1, null))),
            new ReglaPrioridad(Prioridad.BAJA, List.of(
                    clave("no es urgente", 1, null),
                    clave("no corre prisa", 1, null),
                    clave("sin prisa", 1, null),
                    clave("cuando puedan", 1, null),
                    clave("cuando tengan tiempo", 1, null))),
            new ReglaPrioridad(Prioridad.ALTA, List.of(
                    clave("urgente*", 1, "urgente"),
                    clave("urgencia", 1, "urgente"),
                    clave("urge", 1, "urgente"),
                    clave("inmediat*", 1, "urgente"),
                    clave("lo antes posible", 1, "urgente"),
                    clave("cuanto antes", 1, "urgente"),
                    clave("asap", 1, "urgente"),
                    clave("no puedo trabajar", 1, "bloqueo de trabajo"),
                    clave("no puedo continuar", 1, "bloqueo de trabajo"),
                    clave("importante", 1, null),
                    expresion("en \\d+ (minutos|min)", 1, null))),
            new ReglaPrioridad(Prioridad.BAJA, List.of(
                    clave("consulta*", 1, "consulta"),
                    clave("pregunta*", 1, "consulta"),
                    clave("duda*", 1, "consulta"),
                    clave("quisiera saber", 1, "consulta"),
                    clave("sugerencia*", 1, "mejora"),
                    clave("mejora*", 1, "mejora"))));

    /** Contexto útil como etiqueta aunque no influya en la categoría ni en la prioridad. */
    private static final List<PalabraClave> CLAVES_DE_CONTEXTO = List.of(
            clave("reunion*", 1, "reunión"),
            clave("cliente*", 1, "cliente"),
            clave("presentacion*", 1, "presentación"),
            clave("correo*", 1, "correo"));

    @Override
    public AnalisisTicketResponse analizar(String titulo, String descripcion) {
        String texto = NormalizadorTexto.paraComparar(titulo + " " + descripcion);
        ResultadoCategoria categoria = clasificarCategoria(texto);
        ResultadoPrioridad prioridad = clasificarPrioridad(texto);

        Set<String> etiquetas = new LinkedHashSet<>();
        etiquetas.add(categoria.etiqueta());
        etiquetas.addAll(categoria.etiquetasCoincidentes());
        etiquetas.addAll(etiquetasDe(coincidencias(CLAVES_DE_CONTEXTO, texto)));
        etiquetas.addAll(prioridad.etiquetasCoincidentes());

        return new AnalisisTicketResponse(categoria.categoria(), prioridad.prioridad(), resumir(descripcion),
                etiquetas.stream().limit(MAX_ETIQUETAS).toList(), OrigenAnalisis.REGLAS);
    }

    private static ResultadoCategoria clasificarCategoria(String texto) {
        ResultadoCategoria mejor = new ResultadoCategoria(Categoria.OTRO, ETIQUETA_SIN_CATEGORIA, 0, List.of());
        for (ReglaCategoria regla : REGLAS_CATEGORIA) {
            List<PalabraClave> encontradas = coincidencias(regla.claves(), texto);
            int puntaje = encontradas.stream().mapToInt(PalabraClave::peso).sum();
            if (puntaje > mejor.puntaje()) {
                mejor = new ResultadoCategoria(regla.categoria(), regla.etiqueta(), puntaje, etiquetasDe(encontradas));
            }
        }
        return mejor;
    }

    private static ResultadoPrioridad clasificarPrioridad(String texto) {
        for (ReglaPrioridad regla : REGLAS_PRIORIDAD) {
            List<PalabraClave> encontradas = coincidencias(regla.claves(), texto);
            if (!encontradas.isEmpty()) {
                return new ResultadoPrioridad(regla.prioridad(), etiquetasDe(encontradas));
            }
        }
        return new ResultadoPrioridad(Prioridad.MEDIA, List.of());
    }

    /** Usa la primera oración de la descripción; si es muy corta, la descripción completa. Máximo 30 palabras. */
    private static String resumir(String descripcion) {
        String limpia = NormalizadorTexto.colapsarEspacios(descripcion);
        Matcher finDeOracion = FIN_DE_ORACION.matcher(limpia);
        String primeraOracion = finDeOracion.find() ? limpia.substring(0, finDeOracion.start() + 1) : limpia;
        String base = NormalizadorTexto.contarPalabras(primeraOracion) < MIN_PALABRAS_PRIMERA_ORACION
                ? limpia
                : primeraOracion;
        return NormalizadorTexto.limitarPalabras(base, MAX_PALABRAS_RESUMEN);
    }

    private static List<PalabraClave> coincidencias(List<PalabraClave> claves, String texto) {
        return claves.stream().filter(clave -> clave.patron().matcher(texto).find()).toList();
    }

    private static List<String> etiquetasDe(List<PalabraClave> claves) {
        return claves.stream().map(PalabraClave::etiqueta).filter(Objects::nonNull).distinct().toList();
    }

    /** Palabra o frase completa ya normalizada; un "*" final acepta cualquier terminación (conect* → conectarme). */
    private static PalabraClave clave(String texto, int peso, String etiqueta) {
        boolean esPrefijo = texto.endsWith("*");
        String base = esPrefijo ? texto.substring(0, texto.length() - 1) : texto;
        String sufijo = esPrefijo ? "[\\p{L}\\p{N}]*" : "";
        return expresion(Pattern.quote(base) + sufijo, peso, etiqueta);
    }

    private static PalabraClave expresion(String regex, int peso, String etiqueta) {
        return new PalabraClave(Pattern.compile(INICIO_PALABRA + regex + FIN_PALABRA), peso, etiqueta);
    }

    private record PalabraClave(Pattern patron, int peso, String etiqueta) {
    }

    private record ReglaCategoria(Categoria categoria, String etiqueta, List<PalabraClave> claves) {
    }

    private record ReglaPrioridad(Prioridad prioridad, List<PalabraClave> claves) {
    }

    private record ResultadoCategoria(Categoria categoria, String etiqueta, int puntaje,
                                      List<String> etiquetasCoincidentes) {
    }

    private record ResultadoPrioridad(Prioridad prioridad, List<String> etiquetasCoincidentes) {
    }
}
