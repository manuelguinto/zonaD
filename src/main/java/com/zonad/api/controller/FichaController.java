package com.zonad.api.controller;

import com.zonad.api.service.FichaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fichas")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @PostMapping("/vender")
    public ResponseEntity<?> venderFicha() {
        try {
            Map<String, Object> ficha = fichaService.venderFicha();

            if (ficha == null) {
                return ResponseEntity.status(404).body(
                        Map.of(
                                "ok", false,
                                "mensaje", "No hay fichas con estatus NUEVA"
                        )
                );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "ok", true,
                            "ficha", ficha
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "ok", false,
                            "mensaje", "Error al obtener la ficha",
                            "error", e.getMessage() == null ? "Error interno" : e.getMessage()
                    )
            );
        }
    }
}
