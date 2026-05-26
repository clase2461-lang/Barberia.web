package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * DAO para consultas de reportes y métricas de negocio.
 */
public class ReporteDAO {

    private static final Logger log = LoggerFactory.getLogger(ReporteDAO.class);

    /**
     * Ganancias diarias en un rango de fechas.
     * Retorna mapa: fecha -> monto total.
     */
    public Map<LocalDate, BigDecimal> getGananciasPorDia(LocalDate desde, LocalDate hasta) {
        Map<LocalDate, BigDecimal> resultado = new LinkedHashMap<>();
        String sql = "SELECT DATE(p.fecha_pago) AS dia, SUM(p.monto) AS total " +
                     "FROM pagos p " +
                     "JOIN citas c ON p.cita_id = c.id " +
                     "WHERE DATE(p.fecha_pago) BETWEEN ? AND ? " +
                     "GROUP BY dia ORDER BY dia";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate fecha = rs.getDate("dia").toLocalDate();
                    resultado.put(fecha, rs.getBigDecimal("total"));
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener ganancias por día: {}", e.getMessage());
        }
        return resultado;
    }

    /**
     * Ganancias mensuales para un año dado.
     * Retorna mapa: mes (1-12) -> monto total.
     */
    public Map<Integer, BigDecimal> getGananciasPorMes(int anio) {
        Map<Integer, BigDecimal> resultado = new LinkedHashMap<>();
        String sql = "SELECT MONTH(p.fecha_pago) AS mes, SUM(p.monto) AS total " +
                     "FROM pagos p " +
                     "WHERE YEAR(p.fecha_pago) = ? " +
                     "GROUP BY mes ORDER BY mes";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getInt("mes"), rs.getBigDecimal("total"));
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener ganancias por mes: {}", e.getMessage());
        }
        return resultado;
    }

    /**
     * Resumen de citas por estado en un rango de fechas.
     */
    public Map<String, Integer> getCitasPorEstado(LocalDate desde, LocalDate hasta) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        String sql = "SELECT estado, COUNT(*) AS total FROM citas " +
                     "WHERE DATE(fecha_hora) BETWEEN ? AND ? " +
                     "GROUP BY estado";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString("estado"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener citas por estado: {}", e.getMessage());
        }
        return resultado;
    }

    /**
     * Servicios más solicitados.
     */
    public List<Object[]> getServiciosPopulares(LocalDate desde, LocalDate hasta) {
        List<Object[]> resultado = new ArrayList<>();
        String sql = "SELECT s.nombre, COUNT(c.id) AS total_citas, SUM(p.monto) AS total_ingresos " +
                     "FROM citas c " +
                     "JOIN servicios s ON c.servicio_id = s.id " +
                     "LEFT JOIN pagos p ON p.cita_id = c.id " +
                     "WHERE DATE(c.fecha_hora) BETWEEN ? AND ? AND c.estado = 'COMPLETADA' " +
                     "GROUP BY s.id, s.nombre ORDER BY total_citas DESC LIMIT 10";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Object[]{
                        rs.getString("nombre"),
                        rs.getInt("total_citas"),
                        rs.getBigDecimal("total_ingresos")
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener servicios populares: {}", e.getMessage());
        }
        return resultado;
    }

    /**
     * Total ganado en un período.
     */
    public BigDecimal getTotalGanancias(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE DATE(fecha_pago) BETWEEN ? AND ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            log.error("Error al obtener total ganancias: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Citas detalladas para reporte tabular.
     * Retorna lista de arrays: [id, cliente, servicio, fecha, estado, monto].
     */
    public List<Object[]> getCitasDetalladas(LocalDate desde, LocalDate hasta) {
        List<Object[]> resultado = new ArrayList<>();
        String sql = "SELECT c.id, CONCAT(cl.nombre,' ',cl.apellido) AS cliente, " +
                     "s.nombre AS servicio, c.fecha_hora, c.estado, " +
                     "COALESCE(p.monto, s.precio) AS monto, c.canal_reserva " +
                     "FROM citas c " +
                     "JOIN clientes cl ON c.cliente_id = cl.id " +
                     "JOIN servicios s ON c.servicio_id = s.id " +
                     "LEFT JOIN pagos p ON p.cita_id = c.id " +
                     "WHERE DATE(c.fecha_hora) BETWEEN ? AND ? " +
                     "ORDER BY c.fecha_hora";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("cliente"),
                        rs.getString("servicio"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getString("estado"),
                        rs.getBigDecimal("monto"),
                        rs.getString("canal_reserva")
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener citas detalladas: {}", e.getMessage());
        }
        return resultado;
    }

    /**
     * KPIs del dashboard: citas hoy, esta semana, este mes y total clientes.
     */
    public Map<String, Object> getDashboardKPIs() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Citas hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM citas WHERE DATE(fecha_hora) = CURDATE() AND estado != 'CANCELADA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) kpis.put("citasHoy", rs.getInt(1));
            }
            // Citas esta semana
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM citas WHERE YEARWEEK(fecha_hora, 1) = YEARWEEK(NOW(), 1) AND estado != 'CANCELADA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) kpis.put("citasSemana", rs.getInt(1));
            }
            // Ganancias hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE DATE(fecha_pago) = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) kpis.put("gananciasHoy", rs.getBigDecimal(1));
            }
            // Ganancias este mes
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE YEAR(fecha_pago) = YEAR(NOW()) AND MONTH(fecha_pago) = MONTH(NOW())");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) kpis.put("gananciasMes", rs.getBigDecimal(1));
            }
            // Total clientes
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM clientes WHERE activo = TRUE");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) kpis.put("totalClientes", rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Error al obtener KPIs: {}", e.getMessage());
        }
        return kpis;
    }
}
