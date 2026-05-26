package com.barberia.service;

import com.barberia.config.AppConfig;
import com.barberia.model.Cita;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

/**
 * Servicio para envío de notificaciones WhatsApp vía Twilio.
 * Inicializa Twilio automáticamente con las credenciales de config.properties.
 */
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy 'a las' HH:mm",
            java.util.Locale.forLanguageTag("es-MX"));
    private boolean twilioInicializado = false;

    public NotificacionService() {
        initTwilio();
    }

    private void initTwilio() {
        AppConfig cfg = AppConfig.getInstance();
        String sid = cfg.getTwilioAccountSid();
        String token = cfg.getTwilioAuthToken();
        if (sid.isEmpty() || sid.startsWith("CONFIGURA") || token.isEmpty()) {
            log.warn("Twilio no configurado. Las notificaciones WhatsApp estarán deshabilitadas.");
            return;
        }
        try {
            Twilio.init(sid, token);
            twilioInicializado = true;
            log.info("Twilio inicializado correctamente.");
        } catch (Exception e) {
            log.error("Error al inicializar Twilio: {}", e.getMessage());
        }
    }

    /**
     * Envía un mensaje WhatsApp a un número con formato +52XXXXXXXXXX.
     */
    public boolean enviarMensaje(String telefonoDestino, String mensaje) {
        if (!twilioInicializado) {
            log.warn("Twilio no inicializado. Mensaje no enviado a: {}", telefonoDestino);
            return false;
        }
        try {
            AppConfig cfg = AppConfig.getInstance();
            String from = cfg.getTwilioWhatsAppFrom();
            String to = "whatsapp:" + telefonoDestino;

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    mensaje
            ).create();

            log.info("Mensaje enviado a {} - SID: {}", telefonoDestino, message.getSid());
            return true;
        } catch (Exception e) {
            log.error("Error al enviar mensaje WhatsApp a {}: {}", telefonoDestino, e.getMessage());
            return false;
        }
    }

    /**
     * Envía confirmación de cita al cliente.
     */
    public void enviarConfirmacionCita(Cita cita) {
        if (cita.getCliente() == null || cita.getCliente().getTelefono() == null) return;

        String nombre = cita.getCliente().getNombre();
        String servicio = cita.getServicio().getNombre();
        String fecha = cita.getFechaHora().format(FMT);
        String precio = cita.getServicio().getPrecioFormateado();
        String barberia = AppConfig.getInstance().getNombreBarberia();

        String mensaje = String.format(
            "✂️ *%s*\n\n" +
            "¡Hola %s! Tu cita ha sido *confirmada* ✅\n\n" +
            "📋 *Servicio:* %s\n" +
            "📅 *Fecha:* %s\n" +
            "💰 *Precio:* %s\n\n" +
            "Si necesitas cancelar o modificar tu cita, responde a este mensaje.\n\n" +
            "_¡Te esperamos!_ 💈",
            barberia, nombre, servicio, fecha, precio
        );

        enviarMensaje(cita.getCliente().getTelefono(), mensaje);
    }

    /**
     * Envía recordatorio de cita (24h antes).
     */
    public void enviarRecordatorio(Cita cita) {
        if (cita.getCliente() == null || cita.getCliente().getTelefono() == null) return;

        String nombre = cita.getCliente().getNombre();
        String servicio = cita.getServicio().getNombre();
        String fecha = cita.getFechaHora().format(FMT);

        String mensaje = String.format(
            "⏰ *Recordatorio de cita*\n\n" +
            "Hola %s, te recordamos que mañana tienes:\n\n" +
            "✂️ *%s*\n" +
            "📅 *%s*\n\n" +
            "Si no puedes asistir, por favor avísanos. ¡Gracias! 💈",
            nombre, servicio, fecha
        );

        enviarMensaje(cita.getCliente().getTelefono(), mensaje);
    }

    /**
     * Envía notificación de cancelación al cliente.
     */
    public void enviarCancelacion(Cita cita) {
        if (cita.getCliente() == null || cita.getCliente().getTelefono() == null) return;

        String mensaje = String.format(
            "❌ *Cita cancelada*\n\n" +
            "Hola %s, tu cita de *%s* programada para el %s ha sido cancelada.\n\n" +
            "Para reagendar, escríbenos aquí. 💈",
            cita.getCliente().getNombre(),
            cita.getServicio().getNombre(),
            cita.getFechaHora().format(FMT)
        );

        enviarMensaje(cita.getCliente().getTelefono(), mensaje);
    }

    public boolean isTwilioDisponible() {
        return twilioInicializado;
    }
}
