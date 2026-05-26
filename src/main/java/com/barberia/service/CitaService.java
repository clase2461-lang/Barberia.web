package com.barberia.service;

import com.barberia.config.AppConfig;
import com.barberia.dao.CitaDAO;
import com.barberia.model.Cita;
import com.barberia.model.Cliente;
import com.barberia.model.Servicio;
import com.barberia.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio para gestión de citas.
 * Valida disponibilidad, horarios y reglas de negocio.
 */
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);
    private final CitaDAO citaDAO = new CitaDAO();
    private final NotificacionService notificacionService = new NotificacionService();

    /**
     * Crea una nueva cita validando disponibilidad y horario.
     * @throws IllegalArgumentException si el slot no está disponible o está fuera de horario.
     */
    public Cita crearCita(Cliente cliente, Servicio servicio, LocalDateTime fechaHora,
                          Usuario barbero, String notas, Cita.CanalReserva canal) {

        validarHorario(fechaHora, servicio.getDuracionMinutos());

        LocalDateTime fechaFin = fechaHora.plusMinutes(servicio.getDuracionMinutos());
        if (!citaDAO.isSlotDisponible(fechaHora, fechaFin, 0)) {
            throw new IllegalArgumentException("El horario solicitado no está disponible.");
        }

        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setServicio(servicio);
        cita.setFechaHora(fechaHora);
        cita.setFechaFin(fechaFin);
        cita.setBarbero(barbero);
        cita.setNotas(notas);
        cita.setCanalReserva(canal);
        cita.setEstado(Cita.Estado.PENDIENTE);

        return citaDAO.save(cita);
    }

    /**
     * Confirma una cita y envía notificación WhatsApp al cliente.
     */
    public void confirmarCita(int citaId) {
        citaDAO.updateEstado(citaId, Cita.Estado.CONFIRMADA);
        citaDAO.findById(citaId).ifPresent(cita -> {
            try {
                notificacionService.enviarConfirmacionCita(cita);
                citaDAO.marcarNotificado(citaId);
            } catch (Exception e) {
                log.warn("No se pudo enviar notificación para cita {}: {}", citaId, e.getMessage());
            }
        });
    }

    /**
     * Cancela una cita con motivo opcional.
     */
    public void cancelarCita(int citaId, String motivo) {
        citaDAO.findById(citaId).ifPresent(cita -> {
            if (!cita.isActiva()) {
                throw new IllegalStateException("La cita no puede cancelarse en estado: " + cita.getEstado());
            }
            if (motivo != null && !motivo.isEmpty()) {
                cita.setNotas((cita.getNotas() != null ? cita.getNotas() + "\n" : "") + "Cancelación: " + motivo);
            }
            cita.setEstado(Cita.Estado.CANCELADA);
            citaDAO.save(cita);
            log.info("Cita {} cancelada", citaId);
        });
    }

    /**
     * Modifica la fecha/hora de una cita existente.
     */
    public void modificarFechaCita(int citaId, LocalDateTime nuevaFechaHora) {
        citaDAO.findById(citaId).ifPresent(cita -> {
            if (!cita.isPendienteOConfirmada()) {
                throw new IllegalStateException("Solo se pueden modificar citas pendientes o confirmadas.");
            }
            validarHorario(nuevaFechaHora, cita.getServicio().getDuracionMinutos());
            LocalDateTime nuevaFin = nuevaFechaHora.plusMinutes(cita.getServicio().getDuracionMinutos());
            if (!citaDAO.isSlotDisponible(nuevaFechaHora, nuevaFin, citaId)) {
                throw new IllegalArgumentException("El nuevo horario no está disponible.");
            }
            cita.setFechaHora(nuevaFechaHora);
            cita.setFechaFin(nuevaFin);
            cita.setEstado(Cita.Estado.PENDIENTE); // Vuelve a PENDIENTE tras modificar
            cita.setNotificado(false);
            citaDAO.save(cita);
        });
    }

    /**
     * Marca una cita como completada.
     */
    public void completarCita(int citaId) {
        citaDAO.updateEstado(citaId, Cita.Estado.COMPLETADA);
    }

    /**
     * Obtiene los slots de tiempo disponibles para una fecha y servicio dado.
     */
    public List<LocalDateTime> getSlotsDisponibles(LocalDate fecha, Servicio servicio) {
        AppConfig cfg = AppConfig.getInstance();
        List<LocalDateTime> slots = new ArrayList<>();

        LocalTime apertura = LocalTime.parse(cfg.getHorarioApertura());
        LocalTime cierre = LocalTime.parse(cfg.getHorarioCierre());
        int duracion = servicio.getDuracionMinutos();
        int paso = cfg.getDuracionSlot();

        LocalTime hora = apertura;
        while (!hora.plusMinutes(duracion).isAfter(cierre)) {
            LocalDateTime inicio = LocalDateTime.of(fecha, hora);
            LocalDateTime fin = inicio.plusMinutes(duracion);
            if (inicio.isAfter(LocalDateTime.now()) &&
                    citaDAO.isSlotDisponible(inicio, fin, 0)) {
                slots.add(inicio);
            }
            hora = hora.plusMinutes(paso);
        }
        return slots;
    }

    public List<Cita> getCitasPorFecha(LocalDate fecha) {
        return citaDAO.findByFecha(fecha);
    }

    public List<Cita> getCitasPorRango(LocalDateTime desde, LocalDateTime hasta) {
        return citaDAO.findByRangoFechas(desde, hasta);
    }

    public java.util.Optional<Cita> getCitaById(int id) {
        return citaDAO.findById(id);
    }

    private void validarHorario(LocalDateTime fechaHora, int duracionMinutos) {
        AppConfig cfg = AppConfig.getInstance();
        LocalTime apertura = LocalTime.parse(cfg.getHorarioApertura());
        LocalTime cierre = LocalTime.parse(cfg.getHorarioCierre());

        LocalTime hora = fechaHora.toLocalTime();
        LocalTime fin = hora.plusMinutes(duracionMinutos);

        if (hora.isBefore(apertura) || fin.isAfter(cierre)) {
            throw new IllegalArgumentException(String.format(
                    "El horario debe estar entre %s y %s.", apertura, cierre));
        }

        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden crear citas en el pasado.");
        }
    }
}
