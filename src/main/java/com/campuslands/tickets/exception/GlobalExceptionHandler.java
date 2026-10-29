package com.campuslands.tickets.exception;

import com.campuslands.tickets.dto.ErrorResponse;
import com.campuslands.tickets.dto.ErrorResponse.ErrorCampo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                             HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), solicitud);
    }

    @ExceptionHandler(TecnicoInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarTecnicoInvalido(TecnicoInvalidoException ex,
                                                                HttpServletRequest solicitud) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), solicitud);
    }

    @ExceptionHandler({TransicionEstadoInvalidaException.class, OperacionNoPermitidaException.class,
            CorreoDuplicadoException.class})
    public ResponseEntity<ErrorResponse> manejarConflicto(RuntimeException ex, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), solicitud);
    }

    @ExceptionHandler(ProveedorIaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarProveedorIa(ProveedorIaNoDisponibleException ex,
                                                            HttpServletRequest solicitud) {
        log.warn("Proveedor de IA no disponible: {}", ex.getMessage());
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de inteligencia artificial no está disponible. " + ex.getMessage(), solicitud);
    }

    @ExceptionHandler(RespuestaIaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarRespuestaIa(RespuestaIaInvalidaException ex,
                                                            HttpServletRequest solicitud) {
        log.warn("Respuesta inválida del modelo de IA: {}", ex.getMessage());
        return responder(HttpStatus.BAD_GATEWAY,
                "El modelo de inteligencia artificial devolvió una respuesta inválida: " + ex.getMessage(), solicitud);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                           HttpServletRequest solicitud) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorCampo::campo).thenComparing(ErrorCampo::mensaje))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", solicitud, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex,
                                                               HttpServletRequest solicitud) {
        InvalidFormatException formatoInvalido = buscarCausa(ex, InvalidFormatException.class);
        if (formatoInvalido != null && formatoInvalido.getTargetType() != null
                && formatoInvalido.getTargetType().isEnum() && !formatoInvalido.getPath().isEmpty()) {
            String campo = formatoInvalido.getPath().getLast().getPropertyName();
            String mensaje = "Valor inválido '%s' para el campo '%s'. Valores permitidos: %s"
                    .formatted(formatoInvalido.getValue(), campo, valoresPermitidos(formatoInvalido.getTargetType()));
            return responder(HttpStatus.BAD_REQUEST, mensaje, solicitud);
        }
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", solicitud);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest solicitud) {
        String mensaje = "Valor inválido '%s' para el parámetro '%s'".formatted(ex.getValue(), ex.getName());
        Class<?> tipoEsperado = ex.getRequiredType();
        if (tipoEsperado != null && tipoEsperado.isEnum()) {
            mensaje += ". Valores permitidos: " + valoresPermitidos(tipoEsperado);
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje, solicitud);
    }

    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<ErrorResponse> manejarOrdenamientoInvalido(RuntimeException ex,
                                                                     HttpServletRequest solicitud) {
        PropertyReferenceException propiedadInvalida = buscarCausa(ex, PropertyReferenceException.class);
        if (propiedadInvalida == null) {
            return manejarGeneral(ex, solicitud);
        }
        return responder(HttpStatus.BAD_REQUEST,
                "No se puede ordenar por la propiedad '%s'".formatted(propiedadInvalida.getPropertyName()), solicitud);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex,
                                                           HttpServletRequest solicitud) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de los datos",
                solicitud);
    }

    /** Errores propios de Spring MVC (405, 415, 404 de rutas, parámetros faltantes...) y errores no previstos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGeneral(Exception ex, HttpServletRequest solicitud) {
        if (ex instanceof org.springframework.web.ErrorResponse errorDeSpring) {
            HttpStatus estado = HttpStatus.valueOf(errorDeSpring.getStatusCode().value());
            return responder(estado, describirErrorDeSpring(ex, errorDeSpring, solicitud), solicitud);
        }
        log.error("Error no controlado en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado", solicitud);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estado, String mensaje, HttpServletRequest solicitud) {
        return responder(estado, mensaje, solicitud, List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estado, String mensaje, HttpServletRequest solicitud,
                                                    List<ErrorCampo> errores) {
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), estado.value(), estado.name(), mensaje,
                solicitud.getRequestURI(), errores);
        return ResponseEntity.status(estado).body(cuerpo);
    }

    private static String describirErrorDeSpring(Exception ex, org.springframework.web.ErrorResponse errorDeSpring,
                                                 HttpServletRequest solicitud) {
        return switch (ex) {
            case NoResourceFoundException e -> "No existe el recurso " + solicitud.getRequestURI();
            case HttpRequestMethodNotSupportedException e ->
                    "El método %s no está permitido en esta ruta".formatted(e.getMethod());
            case HttpMediaTypeNotSupportedException e ->
                    "Tipo de contenido no soportado: %s. Use application/json".formatted(e.getContentType());
            case MissingServletRequestParameterException e ->
                    "Falta el parámetro obligatorio '%s'".formatted(e.getParameterName());
            default -> {
                String detalle = errorDeSpring.getBody().getDetail();
                yield detalle != null ? detalle : HttpStatus.valueOf(errorDeSpring.getStatusCode().value()).getReasonPhrase();
            }
        };
    }

    private static String valoresPermitidos(Class<?> tipoEnum) {
        return Arrays.stream(tipoEnum.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
    }

    private static <T extends Throwable> T buscarCausa(Throwable error, Class<T> tipo) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
        }
        return null;
    }
}
