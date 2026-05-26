package com.barberia.whatsapp;

import com.barberia.config.AppConfig;
import com.barberia.dao.CitaDAO;
import com.barberia.dao.ClienteDAO;
import com.barberia.dao.ServicioDAO;
import com.barberia.model.Cita;
import com.barberia.model.Cliente;
import com.barberia.model.Servicio;
import com.barberia.service.CitaService;
import com.barberia.service.NotificacionService;
import com.barberia.whatsapp.SessionManager.ChatSession;
import com.barberia.whatsapp.SessionManager.ChatSession.Estado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Motor del chatbot de WhatsApp.
 * Implementa una máquina de estados para guiar al usuario en el proceso de reserva.
 *
 * Flujo principal:
 *   1. INICIO → detectar si el cliente ya existe, si no, registrarlo
 *   2. MENU_PRINCIPAL → mostrar opciones
 *   3. RESERVAR → seleccionar servicio → fecha → hora → confirmar
 *   4. CONSULTAR → ver citas activas
 *   5. CANCELAR → seleccionar cita a cancelar
 */
public class ChatbotHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatbotHandler.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final CitaDAO citaDAO = new CitaDAO();
    private final CitaService citaService = new CitaService();
    private final NotificacionService notificacionService = new NotificacionService();

    /**
     * Punto de entrada principal. Recibe un mensaje y devuelve la respuesta del bot.
     * @param telefono Número de teléfono del remitente (formato +52XXXXXXXXXX)
     * @param mensaje  Texto del mensaje recibido
     * @return Texto de respuesta del bot
     */
    public String procesar(String telefono, String mensaje) {
        log.info("Mensaje recibido de {}: {}", telefono, mensaje);
        mensaje = mensaje.trim();

        ChatSession session = sessionManager.getOrCreate(telefono);

        // Comandos globales
        if (esCancelacionGlobal(mensaje)) {
            session.reset();
            return getMenuPrincipal();
        }

        return switch (session.getEstado()) {
            case INICIO -> handleInicio(session, telefono, mensaje);
            case REGISTRAR_NOMBRE -> handleRegistrarNombre(session, mensaje);
            case REGISTRAR_APELLIDO -> handleRegistrarApellido(session, telefono, mensaje);
            case MENU_PRINCIPAL -> handleMenuPrincipal(session, mensaje);
            case RESERVAR_SELECCIONAR_SERVICIO -> handleSeleccionarServicio(session, mensaje);
            case RESERVAR_SELECCIONAR_FECHA -> handleSeleccionarFecha(session, mensaje);
            case RESERVAR_SELECCIONAR_HORA -> handleSeleccionarHora(session, mensaje);
            case RESERVAR_CONFIRMAR -> handleConfirmarReserva(session, mensaje);
            case CONSULTAR_CITAS -> handleConsultarCitas(session, mensaje);
            case CANCELAR_CITA -> handleCancelarCita(session, mensaje);
            default -> {
                session.reset();
                yield getMenuPrincipal();
            }
        };
    }

    // ====================== HANDLERS ======================

    private String handleInicio(ChatSession session, String telefono, String mensaje) {
        Optional<Cliente> clienteOpt = clienteDAO.findByTelefono(telefono);

        if (clienteOpt.isPresent()) {
            session.set("cliente", clienteOpt.get());
            session.setEstado(Estado.MENU_PRINCIPAL);
            return String.format("¡Bienvenido de nuevo, *%s*! 👋\n\n%s",
                    clienteOpt.get().getNombre(), getMenuPrincipal());
        } else {
            session.setEstado(Estado.REGISTRAR_NOMBRE);
            return String.format(
                "✂️ *%s*\n\n" +
                "¡Hola! Es tu primera vez con nosotros. 😊\n\n" +
                "Para reservar una cita necesito registrarte. " +
                "Por favor, ¿cuál es tu *nombre*?",
                AppConfig.getInstance().getNombreBarberia()
            );
        }
    }

    private String handleRegistrarNombre(ChatSession session, String nombre) {
        if (nombre.length() < 2) return "Por favor ingresa un nombre válido.";
        session.set("nombre", nombre);
        session.setEstado(Estado.REGISTRAR_APELLIDO);
        return "Perfecto, " + nombre + "! ¿Y tu *apellido*?";
    }

    private String handleRegistrarApellido(ChatSession session, String telefono, String apellido) {
        if (apellido.length() < 2) return "Por favor ingresa un apellido válido.";
        String nombre = session.get("nombre");

        Cliente cliente = new Cliente(nombre, apellido, telefono);
        cliente = clienteDAO.save(cliente);
        session.set("cliente", cliente);
        session.setEstado(Estado.MENU_PRINCIPAL);

        return String.format(
            "¡Registro exitoso! Bienvenido, *%s %s* 🎉\n\n%s",
            nombre, apellido, getMenuPrincipal()
        );
    }

    private String handleMenuPrincipal(ChatSession session, String opcion) {
        return switch (opcion) {
            case "1" -> {
                session.setEstado(Estado.RESERVAR_SELECCIONAR_SERVICIO);
                yield getListaServicios();
            }
            case "2" -> {
                session.setEstado(Estado.CONSULTAR_CITAS);
                yield handleConsultarCitas(session, "");
            }
            case "3" -> {
                session.setEstado(Estado.CANCELAR_CITA);
                yield getCitasActivasParaCancelar(session);
            }
            default -> "⚠️ Opción no válida. " + getMenuPrincipal();
        };
    }

    private String handleSeleccionarServicio(ChatSession session, String opcion) {
        List<Servicio> servicios = servicioDAO.findActivos();
        try {
            int idx = Integer.parseInt(opcion) - 1;
            if (idx < 0 || idx >= servicios.size()) throw new NumberFormatException();
            Servicio servicio = servicios.get(idx);
            session.set("servicio", servicio);
            session.setEstado(Estado.RESERVAR_SELECCIONAR_FECHA);
            return String.format(
                "✅ *%s* seleccionado.\n\n" +
                "📅 ¿Para qué fecha quieres tu cita?\n" +
                "Ingresa la fecha en formato *DD/MM/AAAA*\n" +
                "(ejemplo: %s)",
                servicio.getNombre(),
                LocalDate.now().plusDays(1).format(DATE_FMT)
            );
        } catch (NumberFormatException e) {
            return "⚠️ Por favor ingresa el número del servicio.\n\n" + getListaServicios();
        }
    }

    private String handleSeleccionarFecha(ChatSession session, String fechaStr) {
        try {
            LocalDate fecha = LocalDate.parse(fechaStr, DATE_FMT);
            if (fecha.isBefore(LocalDate.now())) {
                return "⚠️ La fecha debe ser posterior a hoy. Intenta de nuevo:";
            }
            session.set("fecha", fecha);
            Servicio servicio = session.get("servicio");
            List<LocalDateTime> slots = citaService.getSlotsDisponibles(fecha, servicio);

            if (slots.isEmpty()) {
                return "😔 No hay horarios disponibles para el *" + fecha.format(DATE_FMT) + "*.\n\n" +
                       "Ingresa otra fecha (DD/MM/AAAA):";
            }

            session.set("slots", slots);
            session.setEstado(Estado.RESERVAR_SELECCIONAR_HORA);

            StringBuilder sb = new StringBuilder();
            sb.append("🕐 *Horarios disponibles para el ").append(fecha.format(DATE_FMT)).append(":*\n\n");
            for (int i = 0; i < slots.size(); i++) {
                sb.append(i + 1).append(". ").append(slots.get(i).format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
            }
            sb.append("\nEscribe el número del horario que prefieres:");
            return sb.toString();
        } catch (DateTimeParseException e) {
            return "⚠️ Formato inválido. Ingresa la fecha como *DD/MM/AAAA*:";
        }
    }

    private String handleSeleccionarHora(ChatSession session, String opcion) {
        List<LocalDateTime> slots = session.get("slots");
        try {
            int idx = Integer.parseInt(opcion) - 1;
            if (idx < 0 || idx >= slots.size()) throw new NumberFormatException();
            LocalDateTime fechaHora = slots.get(idx);
            session.set("fechaHora", fechaHora);
            session.setEstado(Estado.RESERVAR_CONFIRMAR);

            Cliente cliente = session.get("cliente");
            Servicio servicio = session.get("servicio");

            return String.format(
                "📋 *Resumen de tu cita:*\n\n" +
                "👤 *Cliente:* %s\n" +
                "✂️ *Servicio:* %s\n" +
                "💰 *Precio:* %s\n" +
                "📅 *Fecha:* %s\n" +
                "⏱️ *Duración:* %s\n\n" +
                "¿Confirmas la cita?\n" +
                "1️⃣ Sí, confirmar\n" +
                "2️⃣ No, cancelar",
                cliente.getNombreCompleto(),
                servicio.getNombre(),
                servicio.getPrecioFormateado(),
                fechaHora.format(DATETIME_FMT),
                servicio.getDuracionFormateada()
            );
        } catch (NumberFormatException e) {
            return "⚠️ Por favor elige un número válido de la lista.";
        }
    }

    private String handleConfirmarReserva(ChatSession session, String opcion) {
        if ("1".equals(opcion)) {
            try {
                Cliente cliente = session.get("cliente");
                Servicio servicio = session.get("servicio");
                LocalDateTime fechaHora = session.get("fechaHora");

                Cita cita = citaService.crearCita(
                        cliente, servicio, fechaHora,
                        null, "Reservado por WhatsApp", Cita.CanalReserva.WHATSAPP
                );

                session.reset();
                return String.format(
                    "🎉 *¡Cita reservada exitosamente!*\n\n" +
                    "📋 *Folio #%d*\n" +
                    "✂️ %s\n" +
                    "📅 %s\n\n" +
                    "Recibirás una confirmación cuando tu cita sea aprobada. 💈\n\n" +
                    "¿Algo más en lo que te pueda ayudar?\n%s",
                    cita.getId(), servicio.getNombre(),
                    fechaHora.format(DATETIME_FMT),
                    getMenuPrincipal()
                );
            } catch (Exception e) {
                log.error("Error al crear cita desde chatbot: {}", e.getMessage());
                session.reset();
                return "😔 Ocurrió un error al reservar tu cita. Por favor intenta más tarde o llámanos.";
            }
        } else {
            session.reset();
            return "Reserva cancelada. " + getMenuPrincipal();
        }
    }

    private String handleConsultarCitas(ChatSession session, String msg) {
        Cliente cliente = session.get("cliente");
        List<Cita> citas = citaDAO.findActivasByCliente(cliente.getId());
        session.reset();

        if (citas.isEmpty()) {
            return "📭 No tienes citas activas en este momento.\n\n" + getMenuPrincipal();
        }

        StringBuilder sb = new StringBuilder("📅 *Tus próximas citas:*\n\n");
        for (Cita c : citas) {
            sb.append(String.format("*Folio #%d*\n✂️ %s\n📅 %s\n🔖 Estado: %s\n\n",
                    c.getId(), c.getServicio().getNombre(),
                    c.getFechaHoraFormateada(), c.getEstado().getLabel()));
        }
        sb.append(getMenuPrincipal());
        return sb.toString();
    }

    private String handleCancelarCita(ChatSession session, String opcion) {
        Cliente cliente = session.get("cliente");
        List<Cita> citas = citaDAO.findActivasByCliente(cliente.getId());

        if (citas.isEmpty()) {
            session.reset();
            return "📭 No tienes citas activas para cancelar.\n\n" + getMenuPrincipal();
        }

        if (opcion.isEmpty()) {
            return getCitasActivasParaCancelar(session);
        }

        try {
            int idx = Integer.parseInt(opcion) - 1;
            if (idx < 0 || idx >= citas.size()) throw new NumberFormatException();
            Cita cita = citas.get(idx);
            citaService.cancelarCita(cita.getId(), "Cancelada por el cliente vía WhatsApp");
            session.reset();
            return String.format(
                "✅ Tu cita del *%s* para *%s* ha sido cancelada.\n\n" +
                "Si deseas reagendar, escríbenos. 💈\n\n%s",
                cita.getFechaHoraFormateada(), cita.getServicio().getNombre(),
                getMenuPrincipal()
            );
        } catch (NumberFormatException e) {
            return "⚠️ Elige el número de la cita a cancelar:\n\n" + getCitasActivasParaCancelar(session);
        }
    }

    // ====================== HELPERS ======================

    private String getMenuPrincipal() {
        return String.format(
            "💈 *¿En qué te puedo ayudar?*\n\n" +
            "1️⃣ Reservar una cita\n" +
            "2️⃣ Ver mis citas\n" +
            "3️⃣ Cancelar una cita\n\n" +
            "_Escribe el número de tu opción._"
        );
    }

    private String getListaServicios() {
        List<Servicio> servicios = servicioDAO.findActivos();
        StringBuilder sb = new StringBuilder("✂️ *Nuestros servicios:*\n\n");
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            sb.append(String.format("%d. *%s*\n   💰 %s | ⏱️ %s\n\n",
                    i + 1, s.getNombre(), s.getPrecioFormateado(), s.getDuracionFormateada()));
        }
        sb.append("Escribe el número del servicio que deseas:");
        return sb.toString();
    }

    private String getCitasActivasParaCancelar(ChatSession session) {
        Cliente cliente = session.get("cliente");
        List<Cita> citas = citaDAO.findActivasByCliente(cliente.getId());

        if (citas.isEmpty()) {
            session.reset();
            return "📭 No tienes citas activas.\n\n" + getMenuPrincipal();
        }

        StringBuilder sb = new StringBuilder("❌ *¿Cuál cita deseas cancelar?*\n\n");
        for (int i = 0; i < citas.size(); i++) {
            Cita c = citas.get(i);
            sb.append(String.format("%d. *%s* - %s\n",
                    i + 1, c.getServicio().getNombre(), c.getFechaHoraFormateada()));
        }
        sb.append("\nEscribe el número de la cita:");
        return sb.toString();
    }

    private boolean esCancelacionGlobal(String msg) {
        String lower = msg.toLowerCase();
        return lower.equals("menu") || lower.equals("menú") ||
               lower.equals("inicio") || lower.equals("hola") ||
               lower.equals("ayuda") || lower.equals("help") ||
               lower.equals("0");
    }
}
