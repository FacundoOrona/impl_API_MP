package com.api.mp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.api.mp.entities.ProductoRequestDTO;
import com.api.mp.entities.WebhookDTO;
import com.api.mp.service.MPService;

@RestController
@RequestMapping("/mercadopago")
public class MPController {

    private final MPService mpService;

    public MPController(MPService mpService) {
        this.mpService = mpService;
    }

    @PostMapping("/realizarpago")
    public ResponseEntity<?> realizarPago(@RequestBody ProductoRequestDTO request) {
        try {
            String initPoint = mpService.crearPreferencia(request);
            return ResponseEntity.ok(initPoint);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al crear preferencia: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhook(@RequestBody WebhookDTO webhook) {
        try {
            mpService.procesarWebhook(webhook);
            return ResponseEntity.ok("Webhook procesado correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al procesar el webhook: " + e.getMessage());
        }
    }
    
}
