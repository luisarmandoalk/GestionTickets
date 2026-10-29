package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.ActualizarEstadoRequest;
import com.campuslands.tickets.dto.AsignarTecnicoRequest;
import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.MetricasTicketsResponse;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Registro, consulta y gestión de tickets de soporte")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Crear un ticket", description = "Guarda el ticket y lo clasifica con el analizador de IA")
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody CrearTicketRequest solicitud) {
        TicketResponse creado = ticketService.crear(solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Consultar tickets", description = "Filtros opcionales combinables, con paginación y ordenamiento")
    public PaginaResponse<TicketResponse> listar(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) Prioridad prioridad,
            @Parameter(description = "Id del usuario solicitante")
            @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Id del técnico asignado")
            @RequestParam(required = false) Long tecnicoId,
            @Parameter(description = "Texto a buscar en el título o la descripción")
            @RequestParam(required = false) String texto,
            @ParameterObject
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable paginacion) {
        FiltroTickets filtro = new FiltroTickets(estado, categoria, prioridad, usuarioId, tecnicoId, texto);
        return ticketService.listar(filtro, paginacion);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un ticket", description = "Incluye el análisis realizado por la IA")
    public TicketResponse obtener(@Parameter(description = "Id del ticket", example = "1") @PathVariable Long id) {
        return ticketService.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado",
            description = "ABIERTO → EN_PROCESO → RESUELTO → CERRADO; RESUELTO puede volver a EN_PROCESO")
    public TicketResponse actualizarEstado(
            @Parameter(description = "Id del ticket", example = "1") @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoRequest solicitud) {
        return ticketService.actualizarEstado(id, solicitud.estado());
    }

    @PatchMapping("/{id}/asignar")
    @Operation(summary = "Asignar un técnico", description = "Solo se pueden asignar usuarios con rol SOPORTE")
    public TicketResponse asignarTecnico(
            @Parameter(description = "Id del ticket", example = "1") @PathVariable Long id,
            @Valid @RequestBody AsignarTecnicoRequest solicitud) {
        return ticketService.asignarTecnico(id, solicitud.tecnicoId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un ticket", description = "Eliminación lógica: el ticket deja de aparecer en la API")
    public void eliminar(@Parameter(description = "Id del ticket", example = "1") @PathVariable Long id) {
        ticketService.eliminar(id);
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial de cambios de estado")
    public List<HistorialEstadoResponse> historial(
            @Parameter(description = "Id del ticket", example = "1") @PathVariable Long id) {
        return ticketService.historial(id);
    }

    @GetMapping("/metricas")
    @Operation(summary = "Métricas de tickets", description = "Totales por categoría, prioridad y estado")
    public MetricasTicketsResponse metricas() {
        return ticketService.metricas();
    }
}
