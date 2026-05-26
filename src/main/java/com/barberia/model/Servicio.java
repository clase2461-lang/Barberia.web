package com.barberia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de Servicio ofrecido por la barbería.
 */
public class Servicio {

    private int id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int duracionMinutos;
    private boolean activo;
    private LocalDateTime fechaCreacion;

    public Servicio() {}

    public Servicio(String nombre, String descripcion, BigDecimal precio, int duracionMinutos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
        this.activo = true;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getPrecioFormateado() {
        return "$" + precio.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String getDuracionFormateada() {
        if (duracionMinutos < 60) {
            return duracionMinutos + " min";
        }
        int horas = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;
        return minutos > 0 ? horas + "h " + minutos + "min" : horas + "h";
    }

    @Override
    public String toString() {
        return nombre + " - " + getPrecioFormateado() + " (" + getDuracionFormateada() + ")";
    }
}
