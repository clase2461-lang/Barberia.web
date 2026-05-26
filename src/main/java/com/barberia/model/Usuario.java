package com.barberia.model;

import java.time.LocalDateTime;

/**
 * Modelo de Usuario del sistema (administrador o barbero).
 */
public class Usuario {

    public enum Rol {
        ADMIN, BARBERO
    }

    private int id;
    private String username;
    private String passwordHash;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private Rol rol;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoLogin;

    public Usuario() {}

    public Usuario(String username, String passwordHash, String nombre, String apellido, Rol rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.activo = true;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(LocalDateTime ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean isAdmin() {
        return Rol.ADMIN.equals(rol);
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " [" + rol + "]";
    }
}
