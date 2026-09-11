package com.zonad.api.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
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
        // GENERAR FICHAS Y DESCARGAR CSV PARA ALTAI
        // ============================================================
        //
        // POST /fichas/generar?numFichas=10
        //
        // Ejemplo:
        //
        // /fichas/generar?numFichas=10
        //
        // Genera exactamente 10 fichas,
        // las guarda en Firestore
        // y devuelve un CSV listo para Altai.
        //
        // ============================================================

        @PostMapping(
                value = "/generar",
                produces = "text/csv"
        )
        @SuppressWarnings("CallToPrintStackTrace")
        public ResponseEntity<byte[]> generarFichas(
                @RequestParam("numFichas") int numFichas
        ) {

        try {

                // =====================================================
                // VALIDAR PARÁMETRO
                // =====================================================

                if (numFichas <= 0) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "El número de fichas debe ser mayor a 0"
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        );
                }


                // =====================================================
                // GENERAR LAS FICHAS
                // =====================================================

                List<String> fichas =
                        fichaService.generarFichas(
                                numFichas
                        );


                // =====================================================
                // GENERAR CSV
                // =====================================================

                String csv =
                        generarCsvAltai(
                                fichas
                        );


                // =====================================================
                // NOMBRE DEL ARCHIVO
                // =====================================================

                LocalDate fechaActual =
                        LocalDate.now(
                                ZoneId.of(
                                        "America/Mexico_City"
                                )
                        );


                String nombreArchivo =
                        "ZonaD_"
                                + fechaActual
                                + "_"
                                + fichas.size()
                                + "_fichas.csv";


                // =====================================================
                // DEVOLVER ARCHIVO CSV
                // =====================================================

                return ResponseEntity
                        .ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\""
                                        + nombreArchivo
                                        + "\""
                        )
                        .contentType(
                                MediaType.parseMediaType(
                                        "text/csv"
                                )
                        )
                        .body(
                                csv.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );


        } catch (Exception e) {

                e.printStackTrace();


                return ResponseEntity
                        .internalServerError()
                        .body(
                                (
                                        "Error al generar fichas: "
                                                + e.getMessage()
                                )
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
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
    @SuppressWarnings("CallToPrintStackTrace")
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

        // ============================================================
        // OBTENER MES EN ESPAÑOL
        // ============================================================

        private String obtenerMesEspanol(
                int numeroMes
        ) {

        String[] meses = {
                "Enero",
                "Febrero",
                "Marzo",
                "Abril",
                "Mayo",
                "Junio",
                "Julio",
                "Agosto",
                "Septiembre",
                "Octubre",
                "Noviembre",
                "Diciembre"
        };


        return meses[
                numeroMes - 1
        ];
        }

        // ============================================================
        // GENERAR CSV ALTAI
        // ============================================================

        private String generarCsvAltai(
                List<String> fichas
        ) {

        StringBuilder csv =
                new StringBuilder();


        // =====================================================
        // ENCABEZADO EXACTO DEL CSV DE ALTAI
        // =====================================================

        csv.append(
                "name,user group name,login name,password,"
                        + "data quota(in mb),validity(in minutes),device mac"
        );

        csv.append("\r\n");


        // =====================================================
        // OBTENER FECHA ACTUAL
        // =====================================================

        LocalDate fechaActual =
                LocalDate.now(
                        ZoneId.of(
                                "America/Mexico_City"
                        )
                );


        String mes =
                obtenerMesEspanol(
                        fechaActual.getMonthValue()
                );


        // Ejemplo:
        //
        // CetisSeptiembre-10

        String name =
                "Cetis"
                        + mes
                        + "-"
                        + fechaActual.getDayOfMonth();


        // =====================================================
        // GENERAR FILAS
        // =====================================================

        for (String ficha : fichas) {

                // name
                csv.append(name);
                csv.append(",");


                // user group name
                csv.append("H24");
                csv.append(",");


                // login name
                csv.append(ficha);
                csv.append(",");


                // password
                csv.append(ficha);
                csv.append(",");


                // data quota(in mb)
                // vacío
                csv.append(",");


                // validity(in minutes)
                // vacío
                csv.append(",");


                // device mac
                // vacío


                csv.append("\r\n");
        }


        return csv.toString();
        }    
}