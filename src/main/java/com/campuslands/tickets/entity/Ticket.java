package com.campuslands.tickets.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/**
 * Ticket de soporte. La eliminación es lógica: los tickets marcados como eliminados
 * quedan fuera de todas las consultas gracias a {@link SQLRestriction}.
 */
@Entity
@Table(name = "ticket")
@SQLRestriction("eliminado = false")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_asignado_id")
    private Usuario tecnicoAsignado;

    @Column(name = "resumen_ia", nullable = false, length = 500)
    private String resumenIA;

    @ElementCollection
    @CollectionTable(name = "ticket_etiqueta", joinColumns = @JoinColumn(name = "ticket_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "etiqueta", nullable = false, length = 40)
    private List<String> etiquetasIA = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_analisis", nullable = false, length = 20)
    private OrigenAnalisis origenAnalisis;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    protected Ticket() {
    }

    public Ticket(String titulo, String descripcion, Usuario usuarioSolicitante) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.usuarioSolicitante = usuarioSolicitante;
        this.estado = EstadoTicket.ABIERTO;
    }

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public void aplicarAnalisis(Categoria categoria, Prioridad prioridad, String resumen,
                                List<String> etiquetas, OrigenAnalisis origen) {
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.resumenIA = resumen;
        this.etiquetasIA.clear();
        this.etiquetasIA.addAll(etiquetas);
        this.origenAnalisis = origen;
    }

    public void cambiarEstado(EstadoTicket nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public void asignarTecnico(Usuario tecnico) {
        this.tecnicoAsignado = tecnico;
    }

    public void marcarEliminado() {
        this.eliminado = true;
        this.fechaEliminacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public Usuario getUsuarioSolicitante() {
        return usuarioSolicitante;
    }

    public Usuario getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public String getResumenIA() {
        return resumenIA;
    }

    public List<String> getEtiquetasIA() {
        return etiquetasIA;
    }

    public OrigenAnalisis getOrigenAnalisis() {
        return origenAnalisis;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public LocalDateTime getFechaEliminacion() {
        return fechaEliminacion;
    }
}
