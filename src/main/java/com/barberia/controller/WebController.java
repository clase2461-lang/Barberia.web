package com.barberia.controller;

import com.barberia.dao.CitaDAO;
import com.barberia.dao.ClienteDAO;
import com.barberia.model.Cita;
import com.barberia.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebController {

    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        boolean success = AuthService.getInstance().login(username, password);
        if (success) {
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        AuthService.getInstance().logout();
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (!AuthService.getInstance().isLoggedIn()) {
            return "redirect:/login";
        }
        
        List<Cita> citasHoyList = citaDAO.findByFecha(java.time.LocalDate.now());
        long citasHoy = citasHoyList.stream().filter(c -> c.getEstado() == com.barberia.model.Cita.Estado.PENDIENTE).count();
        double gananciasTotales = citasHoyList.stream()
                .filter(c -> c.getEstado() == com.barberia.model.Cita.Estado.COMPLETADA)
                .mapToDouble(c -> c.getServicio().getPrecio().doubleValue())
                .sum();
        
        model.addAttribute("usuario", AuthService.getInstance().getUsuarioActual().getUsername());
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("ganancias", gananciasTotales);
        model.addAttribute("totalCitas", citasHoyList.size());
        model.addAttribute("totalClientes", clienteDAO.findAll().size());
        
        return "dashboard";
    }

    @GetMapping("/citas")
    public String citas(Model model) {
        if (!AuthService.getInstance().isLoggedIn()) {
            return "redirect:/login";
        }
        
        List<Cita> citasList = citaDAO.findByFecha(java.time.LocalDate.now());
        List<com.barberia.model.Cliente> clientes = clienteDAO.findAll();
        List<com.barberia.model.Servicio> servicios = new com.barberia.dao.ServicioDAO().findAll();
        
        java.util.Map<Integer, String> nombresClientes = new java.util.HashMap<>();
        for (com.barberia.model.Cliente c : clientes) {
            nombresClientes.put(c.getId(), c.getNombre() + " " + c.getApellido());
        }
        
        model.addAttribute("citas", citasList);
        model.addAttribute("clientes", clientes);
        model.addAttribute("servicios", servicios);
        model.addAttribute("nombresClientes", nombresClientes);
        return "citas";
    }

    @PostMapping("/citas/nueva")
    public String nuevaCita(@RequestParam int clienteId, @RequestParam int servicioId,
                            @RequestParam String fecha, @RequestParam String hora,
                            @RequestParam(required=false) String notas) {
        
        if (!AuthService.getInstance().isLoggedIn()) return "redirect:/login";
        
        Cita cita = new Cita();
        
        com.barberia.model.Cliente c = new com.barberia.model.Cliente();
        c.setId(clienteId);
        cita.setCliente(c);
        
        com.barberia.model.Servicio s = new com.barberia.model.Servicio();
        s.setId(servicioId);
        cita.setServicio(s);
        
        java.time.LocalDateTime fh = java.time.LocalDateTime.parse(fecha + "T" + hora);
        cita.setFechaHora(fh);
        cita.setFechaFin(fh.plusMinutes(30)); // default 30 min
        cita.setEstado(Cita.Estado.PENDIENTE);
        cita.setNotas(notas);
        cita.setCanalReserva(Cita.CanalReserva.PANEL);
        
        citaDAO.save(cita);
        return "redirect:/citas";
    }
}
