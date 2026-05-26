package com.barberia.whatsapp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestión de sesiones activas del chatbot por número de teléfono.
 * Mantiene el estado de la conversación de cada usuario en memoria.
 * Las sesiones expiran después de 30 minutos de inactividad.
 */
public class SessionManager {

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutos

    private static SessionManager instance;
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public ChatSession getOrCreate(String telefono) {
        ChatSession session = sessions.get(telefono);
        if (session == null || session.isExpired()) {
            session = new ChatSession(telefono);
            sessions.put(telefono, session);
        } else {
            session.updateLastActivity();
        }
        return session;
    }

    public void remove(String telefono) {
        sessions.remove(telefono);
    }

    public void cleanExpired() {
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /**
     * Representa el estado de la conversación de un usuario en WhatsApp.
     */
    public static class ChatSession {

        public enum Estado {
            INICIO,
            MENU_PRINCIPAL,
            RESERVAR_SELECCIONAR_SERVICIO,
            RESERVAR_SELECCIONAR_FECHA,
            RESERVAR_SELECCIONAR_HORA,
            RESERVAR_CONFIRMAR,
            CONSULTAR_CITAS,
            CANCELAR_CITA,
            MODIFICAR_CITA,
            MODIFICAR_NUEVA_FECHA,
            MODIFICAR_NUEVA_HORA,
            REGISTRAR_NOMBRE,
            REGISTRAR_APELLIDO
        }

        private final String telefono;
        private Estado estado;
        private LocalDateTime ultimaActividad;
        private final Map<String, Object> datos = new HashMap<>();

        public ChatSession(String telefono) {
            this.telefono = telefono;
            this.estado = Estado.INICIO;
            this.ultimaActividad = LocalDateTime.now();
        }

        public boolean isExpired() {
            long elapsed = java.time.Duration.between(ultimaActividad, LocalDateTime.now()).toMillis();
            return elapsed > SESSION_TIMEOUT_MS;
        }

        public void updateLastActivity() {
            this.ultimaActividad = LocalDateTime.now();
        }

        public void reset() {
            this.estado = Estado.MENU_PRINCIPAL;
            this.datos.clear();
        }

        // Getters y Setters
        public String getTelefono() { return telefono; }
        public Estado getEstado() { return estado; }
        public void setEstado(Estado estado) {
            this.estado = estado;
            this.ultimaActividad = LocalDateTime.now();
        }
        public LocalDateTime getUltimaActividad() { return ultimaActividad; }

        public void set(String key, Object value) { datos.put(key, value); }

        @SuppressWarnings("unchecked")
        public <T> T get(String key) { return (T) datos.get(key); }

        public boolean has(String key) { return datos.containsKey(key); }
        public void remove(String key) { datos.remove(key); }
    }
}
