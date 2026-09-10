package com.zonad.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zonad.api.service.FichaService;


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

            Object valorFicha = ficha.get("Ficha");

             if (valorFicha == null) {
                return ResponseEntity.internalServerError()
                        .body(Map.of(
                                "mensaje", "La ficha no contiene el campo Ficha"
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "Ficha", valorFicha.toString()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Throwable causa = e;

            while (causa.getCause() != null) {
                causa = causa.getCause();
            }

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "ok", false,
                            "mensaje", "Error al obtener la ficha",
                            "error", e.getMessage() == null ? "Error interno" : e.getMessage(),
                            "causa", causa.getMessage() == null
                                    ? causa.getClass().getName()
                                    : causa.getMessage()
                    )
            );
        }
    }

    // ============================================================
    // GENERAR 1000 FICHAS
    // ============================================================
    //
    // POST /fichas/generar
    //
    // Respuesta:
    //
    // 211210,584921,740163,193552,...
    //
    // ============================================================

    @PostMapping(
            value = "/generar",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<String> generarFichas() {

        try {

            List<String> fichas =
                    fichaService.generarFichas();


            /*
             * Convertimos:
             *
             * ["211210", "584921", "740163"]
             *
             * en:
             *
             * 211210,584921,740163
             */

            String respuesta =
                    String.join(
                            ",",
                            fichas
                    );


            return ResponseEntity.ok(
                    respuesta
            );


        } catch (Exception e) {

            e.printStackTrace();


            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Error al generar fichas: "
                            + e.getMessage()
                    );
        }
    }

    // ============================================================
// LIMPIAR FICHAS VENDIDAS
// ============================================================
//
// POST /fichas/limpiar
//
// Elimina todas las fichas con:
//
// Estado = "VENDIDA"
//
// ============================================================

@PostMapping(
        value = "/limpiar",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<Map<String, Object>>
limpiarFichasVendidas() {

    try {

        int eliminadas =
                fichaService
                        .limpiarFichasVendidas();


        Map<String, Object> respuesta =
                new HashMap<>();


        respuesta.put(
                "ok",
                true
        );

        respuesta.put(
                "eliminadas",
                eliminadas
        );

        respuesta.put(
                "mensaje",
                "Limpieza completada"
        );


        return ResponseEntity.ok(
                respuesta
        );


    } catch (Exception e) {

        e.printStackTrace();


        Map<String, Object> respuesta =
                new HashMap<>();


        respuesta.put(
                "ok",
                false
        );

        respuesta.put(
                "mensaje",
                e.getMessage()
        );


        return ResponseEntity
                .internalServerError()
                .body(respuesta);
    }
        }
}
