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

@Entity
@Table(name = "historial_estado_ticket")
public class HistorialEstadoTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoTicket estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoTicket estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    protected HistorialEstadoTicket() {
    }

    public HistorialEstadoTicket(Ticket ticket, EstadoTicket estadoAnterior, EstadoTicket estadoNuevo) {
        this.ticket = ticket;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
    }

    @PrePersist
    void alCrear() {
        fechaCambio = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public EstadoTicket getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoTicket getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }
}
