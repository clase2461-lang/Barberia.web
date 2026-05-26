package com.barberia.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo de Cita de la barbería.
 */
public class Cita {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public enum Estado {
        PENDIENTE("Pendiente"),
        CONFIRMADA("Confirmada"),
        EN_PROCESO("En proceso"),
        COMPLETADA("Completada"),
        CANCELADA("Cancelada"),
        NO_ASISTIO("No asistió");

        private final String label;
        Estado(String label) { this.label = label; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    public enum CanalReserva {
        WHATSAPP, PANEL, TELEFONO
    }

    private int id;
    private Cliente cliente;
    private Servicio servicio;
    private Usuario barbero;
    private LocalDateTime fechaHora;
    private LocalDateTime fechaFin;
    private Estado estado;
    private String notas;
    private CanalReserva canalReserva;
    private boolean notificado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    public Cita() {
        this.estado = Estado.PENDIENTE;
        this.canalReserva = CanalReserva.PANEL;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
        if (fechaHora != null && servicio != null) {
            this.fechaFin = fechaHora.plusMinutes(servicio.getDuracionMinutos());
        }
    }

    public Usuario getBarbero() { return barbero; }
    public void setBarbero(Usuario barbero) { this.barbero = barbero; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
        if (servicio != null && fechaHora != null) {
            this.fechaFin = fechaHora.plusMinutes(servicio.getDuracionMinutos());
        }
    }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public CanalReserva getCanalReserva() { return canalReserva; }
    public void setCanalReserva(CanalReserva canalReserva) { this.canalReserva = canalReserva; }

    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    // Helpers
    public String getFechaHoraFormateada() {
        return fechaHora != null ? fechaHora.format(FMT) : "";
    }

    public boolean isActiva() {
        return estado == Estado.PENDIENTE || estado == Estado.CONFIRMADA || estado == Estado.EN_PROCESO;
    }

    public boolean isPendienteOConfirmada() {
        return estado == Estado.PENDIENTE || estado == Estado.CONFIRMADA;
    }

    @Override
    public String toString() {
        return String.format("Cita #%d - %s - %s [%s]",
                id,
                cliente != null ? cliente.getNombreCompleto() : "?",
                getFechaHoraFormateada(),
                estado.getLabel());
    }
}
