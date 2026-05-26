package com.barberia.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {}
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(props.getProperty(key)); } catch(Exception e) { return defaultValue; }
    }

    public String getTwilioAccountSid() { return get("twilio.account.sid", ""); }
    public String getTwilioAuthToken() { return get("twilio.auth.token", ""); }
    public String getTwilioWhatsAppFrom() { return get("twilio.whatsapp.from", "whatsapp:+14155238886"); }
    public int getWebhookPort() { return getInt("server.port", 8080); }
    public String getHorarioApertura() { return get("horario.apertura", "09:00"); }
    public String getHorarioCierre() { return get("horario.cierre", "20:00"); }
    public int getDuracionSlot() { return getInt("horario.duracion_slot", 30); }
    public String getNombreBarberia() { return get("barberia.nombre", "Barbería Juan"); }
}
