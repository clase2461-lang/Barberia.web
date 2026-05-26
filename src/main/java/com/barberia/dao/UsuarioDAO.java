package com.barberia.dao;

import com.barberia.config.DatabaseConfig;
import com.barberia.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de usuarios del sistema.
 */
public class UsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDAO.class);

    private Usuario mapRow(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setTelefono(rs.getString("telefono"));
        u.setEmail(rs.getString("email"));
        u.setRol(Usuario.Rol.valueOf(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) u.setFechaCreacion(ts.toLocalDateTime());
        Timestamp ul = rs.getTimestamp("ultimo_login");
        if (ul != null) u.setUltimoLogin(ul.toLocalDateTime());
        return u;
    }

    public Optional<Usuario> findByUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = TRUE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar usuario: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Usuario> findById(int id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar usuario por ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<Usuario> findAll() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre, apellido";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al listar usuarios: {}", e.getMessage());
        }
        return lista;
    }

    public List<Usuario> findBarberos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE rol = 'BARBERO' AND activo = TRUE ORDER BY nombre";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error al listar barberos: {}", e.getMessage());
        }
        return lista;
    }

    public Usuario save(Usuario usuario) {
        return usuario.getId() == 0 ? insert(usuario) : update(usuario);
    }

    private Usuario insert(Usuario u) {
        String sql = "INSERT INTO usuarios (username, password_hash, nombre, apellido, telefono, email, rol, activo) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombre());
            ps.setString(4, u.getApellido());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getEmail());
            ps.setString(7, u.getRol().name());
            ps.setBoolean(8, u.isActivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Error al insertar usuario: {}", e.getMessage());
            throw new RuntimeException("Error al crear usuario", e);
        }
        return u;
    }

    private Usuario update(Usuario u) {
        String sql = "UPDATE usuarios SET username=?, password_hash=?, nombre=?, apellido=?, telefono=?, email=?, rol=?, activo=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombre());
            ps.setString(4, u.getApellido());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getEmail());
            ps.setString(7, u.getRol().name());
            ps.setBoolean(8, u.isActivo());
            ps.setInt(9, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar usuario: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar usuario", e);
        }
        return u;
    }

    public void updateUltimoLogin(int id) {
        String sql = "UPDATE usuarios SET ultimo_login = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar ultimo login: {}", e.getMessage());
        }
    }
}
