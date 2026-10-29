package com.campuslands.tickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String correo;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, String passwordCifrado, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.password = passwordCifrado;
        this.rol = rol;
    }

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }

    public boolean esTecnicoDeSoporte() {
        return rol == Rol.SOPORTE;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPassword() {
        return password;
    }

    public Rol getRol() {
        return rol;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
