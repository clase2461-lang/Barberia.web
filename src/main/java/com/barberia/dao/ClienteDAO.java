package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import com.barberia.model.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de clientes.
 */
public class ClienteDAO {

    private static final Logger log = LoggerFactory.getLogger(ClienteDAO.class);

    private Cliente mapRow(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setNotas(rs.getString("notas"));
        c.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) c.setFechaRegistro(ts.toLocalDateTime());
        return c;
    }

    public Optional<Cliente> findById(int id) {
        String sql = "SELECT * FROM clientes WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar cliente por ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Cliente> findByTelefono(String telefono) {
        String sql = "SELECT * FROM clientes WHERE telefono = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, telefono);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar cliente por teléfono: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<Cliente> findAll() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE activo = TRUE ORDER BY nombre, apellido";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al listar clientes: {}", e.getMessage());
        }
        return lista;
    }

    public List<Cliente> search(String query) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE activo = TRUE AND " +
                     "(nombre LIKE ? OR apellido LIKE ? OR telefono LIKE ?) " +
                     "ORDER BY nombre, apellido LIMIT 50";
        String q = "%" + query + "%";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar clientes: {}", e.getMessage());
        }
        return lista;
    }

    public Cliente save(Cliente cliente) {
        if (cliente.getId() == 0) {
            return insert(cliente);
        } else {
            return update(cliente);
        }
    }

    private Cliente insert(Cliente c) {
        String sql = "INSERT INTO clientes (nombre, apellido, telefono, email, notas, activo) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getApellido());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getNotas());
            ps.setBoolean(6, c.isActivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            log.info("Cliente creado: {} (ID: {})", c.getNombreCompleto(), c.getId());
        } catch (SQLException e) {
            log.error("Error al insertar cliente: {}", e.getMessage());
            throw new RuntimeException("Error al guardar cliente", e);
        }
        return c;
    }

    private Cliente update(Cliente c) {
        String sql = "UPDATE clientes SET nombre=?, apellido=?, telefono=?, email=?, notas=?, activo=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getApellido());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getNotas());
            ps.setBoolean(6, c.isActivo());
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar cliente: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar cliente", e);
        }
        return c;
    }

    public int countAll() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes WHERE activo = TRUE");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error al contar clientes: {}", e.getMessage());
        }
        return 0;
    }
}
