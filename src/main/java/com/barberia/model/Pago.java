package com.barberia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de Pago asociado a una cita.
 */
public class Pago {

    public enum MetodoPago {
        EFECTIVO("Efectivo"),
        TARJETA("Tarjeta"),
        TRANSFERENCIA("Transferencia"),
        OTRO("Otro");

        private final String label;
        MetodoPago(String label) { this.label = label; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    private int id;
    private Cita cita;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String referencia;
    private String notas;
    private LocalDateTime fechaPago;
    private Usuario registradoPor;

    public Pago() {
        this.metodoPago = MetodoPago.EFECTIVO;
    }

    public Pago(Cita cita, BigDecimal monto, MetodoPago metodoPago) {
        this.cita = cita;
        this.monto = monto;
        this.metodoPago = metodoPago;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }

    public String getMontoFormateado() {
        return "$" + monto.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("Pago #%d - %s - %s",
                id, getMontoFormateado(), metodoPago.getLabel());
    }
}
