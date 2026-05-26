package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import com.barberia.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de citas con joins completos.
 */
public class CitaDAO {

    private static final Logger log = LoggerFactory.getLogger(CitaDAO.class);

    private static final String BASE_SELECT =
        "SELECT c.*, " +
        "cl.nombre AS cl_nombre, cl.apellido AS cl_apellido, cl.telefono AS cl_telefono, cl.email AS cl_email, " +
        "s.nombre AS s_nombre, s.descripcion AS s_desc, s.precio AS s_precio, s.duracion_minutos AS s_duracion, " +
        "u.nombre AS u_nombre, u.apellido AS u_apellido, u.username AS u_username " +
        "FROM citas c " +
        "JOIN clientes cl ON c.cliente_id = cl.id " +
        "JOIN servicios s ON c.servicio_id = s.id " +
        "LEFT JOIN usuarios u ON c.barbero_id = u.id ";

    private Cita mapRow(ResultSet rs) throws SQLException {
        Cita cita = new Cita();
        cita.setId(rs.getInt("id"));

        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("cliente_id"));
        cliente.setNombre(rs.getString("cl_nombre"));
        cliente.setApellido(rs.getString("cl_apellido"));
        cliente.setTelefono(rs.getString("cl_telefono"));
        cliente.setEmail(rs.getString("cl_email"));
        cita.setCliente(cliente);

        Servicio servicio = new Servicio();
        servicio.setId(rs.getInt("servicio_id"));
        servicio.setNombre(rs.getString("s_nombre"));
        servicio.setDescripcion(rs.getString("s_desc"));
        servicio.setPrecio(rs.getBigDecimal("s_precio"));
        servicio.setDuracionMinutos(rs.getInt("s_duracion"));
        cita.setServicio(servicio);

        String uNombre = rs.getString("u_nombre");
        if (uNombre != null) {
            Usuario barbero = new Usuario();
            barbero.setId(rs.getInt("barbero_id"));
            barbero.setNombre(uNombre);
            barbero.setApellido(rs.getString("u_apellido"));
            barbero.setUsername(rs.getString("u_username"));
            cita.setBarbero(barbero);
        }

        Timestamp fh = rs.getTimestamp("fecha_hora");
        if (fh != null) cita.setFechaHora(fh.toLocalDateTime());
        Timestamp ff = rs.getTimestamp("fecha_fin");
        if (ff != null) cita.setFechaFin(ff.toLocalDateTime());

        cita.setEstado(Cita.Estado.valueOf(rs.getString("estado")));
        cita.setNotas(rs.getString("notas"));

        String canal = rs.getString("canal_reserva");
        if (canal != null) cita.setCanalReserva(Cita.CanalReserva.valueOf(canal));

        cita.setNotificado(rs.getBoolean("notificado"));

        Timestamp fc = rs.getTimestamp("fecha_creacion");
        if (fc != null) cita.setFechaCreacion(fc.toLocalDateTime());

        return cita;
    }

    public Optional<Cita> findById(int id) {
        String sql = BASE_SELECT + "WHERE c.id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar cita por ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<Cita> findByFecha(LocalDate fecha) {
        List<Cita> lista = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE DATE(c.fecha_hora) = ? ORDER BY c.fecha_hora";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas por fecha: {}", e.getMessage());
        }
        return lista;
    }

    public List<Cita> findByRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        List<Cita> lista = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE c.fecha_hora BETWEEN ? AND ? ORDER BY c.fecha_hora";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde));
            ps.setTimestamp(2, Timestamp.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas por rango: {}", e.getMessage());
        }
        return lista;
    }

    public List<Cita> findByCliente(int clienteId) {
        List<Cita> lista = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE c.cliente_id = ? ORDER BY c.fecha_hora DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas del cliente: {}", e.getMessage());
        }
        return lista;
    }

    public List<Cita> findActivasByCliente(int clienteId) {
        List<Cita> lista = new ArrayList<>();
        String sql = BASE_SELECT +
                "WHERE c.cliente_id = ? AND c.estado IN ('PENDIENTE','CONFIRMADA') " +
                "AND c.fecha_hora > NOW() ORDER BY c.fecha_hora";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar citas activas del cliente: {}", e.getMessage());
        }
        return lista;
    }

    /**
     * Verifica si un slot de tiempo está disponible (no hay citas solapadas).
     */
    public boolean isSlotDisponible(LocalDateTime inicio, LocalDateTime fin, int excluirCitaId) {
        String sql = "SELECT COUNT(*) FROM citas " +
                     "WHERE estado NOT IN ('CANCELADA', 'NO_ASISTIO') " +
                     "AND id != ? " +
                     "AND NOT (fecha_fin <= ? OR fecha_hora >= ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, excluirCitaId);
            ps.setTimestamp(2, Timestamp.valueOf(inicio));
            ps.setTimestamp(3, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            log.error("Error al verificar disponibilidad: {}", e.getMessage());
        }
        return false;
    }

    public List<Cita> findPendientesDeNotificacion() {
        List<Cita> lista = new ArrayList<>();
        String sql = BASE_SELECT +
                "WHERE c.estado = 'CONFIRMADA' AND c.notificado = FALSE " +
                "AND c.fecha_hora > NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al buscar citas pendientes de notificación: {}", e.getMessage());
        }
        return lista;
    }

    public Cita save(Cita cita) {
        return cita.getId() == 0 ? insert(cita) : update(cita);
    }

    private Cita insert(Cita c) {
        String sql = "INSERT INTO citas (cliente_id, servicio_id, barbero_id, fecha_hora, fecha_fin, estado, notas, canal_reserva) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getCliente().getId());
            ps.setInt(2, c.getServicio().getId());
            if (c.getBarbero() != null) ps.setInt(3, c.getBarbero().getId());
            else ps.setNull(3, Types.INTEGER);
            ps.setTimestamp(4, Timestamp.valueOf(c.getFechaHora()));
            ps.setTimestamp(5, Timestamp.valueOf(c.getFechaFin()));
            ps.setString(6, c.getEstado().name());
            ps.setString(7, c.getNotas());
            ps.setString(8, c.getCanalReserva() != null ? c.getCanalReserva().name() : "PANEL");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            log.info("Cita creada ID: {}", c.getId());
        } catch (SQLException e) {
            log.error("Error al insertar cita: {}", e.getMessage());
            throw new RuntimeException("Error al guardar cita", e);
        }
        return c;
    }

    private Cita update(Cita c) {
        String sql = "UPDATE citas SET servicio_id=?, barbero_id=?, fecha_hora=?, fecha_fin=?, estado=?, notas=?, notificado=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getServicio().getId());
            if (c.getBarbero() != null) ps.setInt(2, c.getBarbero().getId());
            else ps.setNull(2, Types.INTEGER);
            ps.setTimestamp(3, Timestamp.valueOf(c.getFechaHora()));
            ps.setTimestamp(4, Timestamp.valueOf(c.getFechaFin()));
            ps.setString(5, c.getEstado().name());
            ps.setString(6, c.getNotas());
            ps.setBoolean(7, c.isNotificado());
            ps.setInt(8, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar cita: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar cita", e);
        }
        return c;
    }

    public void updateEstado(int id, Cita.Estado estado) {
        String sql = "UPDATE citas SET estado = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar estado de cita: {}", e.getMessage());
        }
    }

    public void marcarNotificado(int id) {
        String sql = "UPDATE citas SET notificado = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al marcar cita como notificada: {}", e.getMessage());
        }
    }

    public int countHoy() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM citas WHERE DATE(fecha_hora) = CURDATE() AND estado != 'CANCELADA'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error al contar citas de hoy: {}", e.getMessage());
        }
        return 0;
    }
}
