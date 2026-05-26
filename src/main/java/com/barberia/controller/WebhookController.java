package com.barberia.controller;

import com.barberia.whatsapp.ChatbotHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final ChatbotHandler chatbotHandler = new ChatbotHandler();

    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveWebhook(@RequestParam Map<String, String> params) {
        try {
            String from = params.getOrDefault("From", "");
            String message = params.getOrDefault("Body", "").trim();
            String telefono = from.replace("whatsapp:", "");

            log.info("Mensaje de {}: {}", telefono, message);

            String respuesta = chatbotHandler.procesar(telefono, message);
            String twiml = buildTwiML(respuesta);

            return ResponseEntity.ok(twiml);
        } catch (Exception e) {
            log.error("Error procesando webhook de WhatsApp: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String buildTwiML(String text) {
        if (text == null) text = "Ocurrió un error en el sistema.";
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<Response>\n" +
               "  <Message>\n" +
               "    <Body>" + escaped + "</Body>\n" +
               "  </Message>\n" +
               "</Response>";
    }
}
