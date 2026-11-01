package com.campuslands.tickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Registro de quién hizo cada cambio sobre un ticket. No se modifica ni se borra. */
@Entity
@Table(name = "auditoria_ticket")
public class AuditoriaTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id en lugar de relación: así la auditoría también se consulta para tickets eliminados. */
    @Column(name = "ticket_id", nullable = false, updatable = false)
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AccionAuditoria accion;

    @Column(nullable = false, length = 500, updatable = false)
    private String detalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected AuditoriaTicket() {
    }

    public AuditoriaTicket(Long ticketId, AccionAuditoria accion, String detalle, Usuario usuario) {
        this.ticketId = ticketId;
        this.accion = accion;
        this.detalle = detalle;
        this.usuario = usuario;
    }

    @PrePersist
    void alCrear() {
        fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
