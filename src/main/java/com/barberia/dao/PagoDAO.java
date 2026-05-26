package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import com.barberia.model.Pago;
import com.barberia.model.Cita;
import com.barberia.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para registro y consulta de pagos.
 */
public class PagoDAO {

    private static final Logger log = LoggerFactory.getLogger(PagoDAO.class);

    public Pago save(Pago pago) {
        String sql = "INSERT INTO pagos (cita_id, monto, metodo_pago, referencia, notas, registrado_por) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pago.getCita().getId());
            ps.setBigDecimal(2, pago.getMonto());
            ps.setString(3, pago.getMetodoPago().name());
            ps.setString(4, pago.getReferencia());
            ps.setString(5, pago.getNotas());
            if (pago.getRegistradoPor() != null) ps.setInt(6, pago.getRegistradoPor().getId());
            else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) pago.setId(keys.getInt(1));
            }
            log.info("Pago registrado ID: {} para cita ID: {}", pago.getId(), pago.getCita().getId());
        } catch (SQLException e) {
            log.error("Error al registrar pago: {}", e.getMessage());
            throw new RuntimeException("Error al registrar pago", e);
        }
        return pago;
    }

    public List<Pago> findByCita(int citaId) {
        List<Pago> lista = new ArrayList<>();
        String sql = "SELECT * FROM pagos WHERE cita_id = ? ORDER BY fecha_pago";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pago p = new Pago();
                    p.setId(rs.getInt("id"));
                    Cita c = new Cita();
                    c.setId(rs.getInt("cita_id"));
                    p.setCita(c);
                    p.setMonto(rs.getBigDecimal("monto"));
                    p.setMetodoPago(Pago.MetodoPago.valueOf(rs.getString("metodo_pago")));
                    p.setReferencia(rs.getString("referencia"));
                    p.setNotas(rs.getString("notas"));
                    Timestamp ts = rs.getTimestamp("fecha_pago");
                    if (ts != null) p.setFechaPago(ts.toLocalDateTime());
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar pagos por cita: {}", e.getMessage());
        }
        return lista;
    }

    public boolean existePagoByCita(int citaId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM pagos WHERE cita_id = ?")) {
            ps.setInt(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Error al verificar pago: {}", e.getMessage());
        }
        return false;
    }
}
