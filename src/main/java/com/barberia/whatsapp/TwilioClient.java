package com.barberia.whatsapp;

import com.barberia.config.AppConfig;
import com.barberia.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cliente Twilio para envío de mensajes WhatsApp proactivos.
 * (Los mensajes reactivos se envían directamente desde WhatsAppWebhook vía TwiML)
 */
public class TwilioClient {

    private static final Logger log = LoggerFactory.getLogger(TwilioClient.class);
    private final NotificacionService notificacionService = new NotificacionService();

    /**
     * Envía un mensaje WhatsApp proactivo a un número de teléfono.
     * Útil para notificaciones iniciadas desde el panel de control.
     */
    public boolean enviarMensaje(String telefono, String mensaje) {
        return notificacionService.enviarMensaje(telefono, mensaje);
    }

    public boolean isTwilioDisponible() {
        return notificacionService.isTwilioDisponible();
    }
}
