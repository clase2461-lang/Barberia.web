package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import com.barberia.model.Servicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de servicios.
 */
public class ServicioDAO {

    private static final Logger log = LoggerFactory.getLogger(ServicioDAO.class);

    private Servicio mapRow(ResultSet rs) throws SQLException {
        Servicio s = new Servicio();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        s.setDescripcion(rs.getString("descripcion"));
        s.setPrecio(rs.getBigDecimal("precio"));
        s.setDuracionMinutos(rs.getInt("duracion_minutos"));
        s.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) s.setFechaCreacion(ts.toLocalDateTime());
        return s;
    }

    public Optional<Servicio> findById(int id) {
        String sql = "SELECT * FROM servicios WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar servicio: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<Servicio> findAll() {
        List<Servicio> lista = new ArrayList<>();
        String sql = "SELECT * FROM servicios ORDER BY nombre";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al listar servicios: {}", e.getMessage());
        }
        return lista;
    }

    public List<Servicio> findActivos() {
        List<Servicio> lista = new ArrayList<>();
        String sql = "SELECT * FROM servicios WHERE activo = TRUE ORDER BY nombre";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al listar servicios activos: {}", e.getMessage());
        }
        return lista;
    }

    public Servicio save(Servicio servicio) {
        return servicio.getId() == 0 ? insert(servicio) : update(servicio);
    }

    private Servicio insert(Servicio s) {
        String sql = "INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, activo) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNombre());
            ps.setString(2, s.getDescripcion());
            ps.setBigDecimal(3, s.getPrecio());
            ps.setInt(4, s.getDuracionMinutos());
            ps.setBoolean(5, s.isActivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            log.info("Servicio creado: {} (ID: {})", s.getNombre(), s.getId());
        } catch (SQLException e) {
            log.error("Error al insertar servicio: {}", e.getMessage());
            throw new RuntimeException("Error al guardar servicio", e);
        }
        return s;
    }

    private Servicio update(Servicio s) {
        String sql = "UPDATE servicios SET nombre=?, descripcion=?, precio=?, duracion_minutos=?, activo=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNombre());
            ps.setString(2, s.getDescripcion());
            ps.setBigDecimal(3, s.getPrecio());
            ps.setInt(4, s.getDuracionMinutos());
            ps.setBoolean(5, s.isActivo());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar servicio: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar servicio", e);
        }
        return s;
    }

    public void toggleActivo(int id) {
        String sql = "UPDATE servicios SET activo = NOT activo WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al cambiar estado del servicio: {}", e.getMessage());
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM servicios WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Puede fallar si tiene citas asociadas (FK), lo cual es correcto
            log.warn("No se pudo eliminar servicio ID {}: {}", id, e.getMessage());
            return false;
        }
    }
}
