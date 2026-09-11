package com.zonad.api.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;




@Service
public class FichaService {

    private static final String COLLECTION = "ZonaDFichas";
    private static final String NUEVA = "NUEVA";
    private static final String VENDIDA = "VENDIDA";

    private final Firestore db;

    private final SecureRandom random = new SecureRandom();

    public FichaService(Firestore db) {
        this.db = db;
    }

    public Map<String, Object> venderFicha()
            throws ExecutionException, InterruptedException {

        return db.runTransaction(transaction -> {

            Query query = db.collection(COLLECTION)
                    .whereEqualTo("Estado", NUEVA)
                    .limit(1);

            QuerySnapshot snapshot = transaction.get(query).get();
            List<QueryDocumentSnapshot> docs = snapshot.getDocuments();

            if (docs.isEmpty()) {
                return null;
            }

            QueryDocumentSnapshot doc = docs.get(0);
            DocumentReference ref = doc.getReference();

            transaction.update(ref, Map.of(
                    "Estado", VENDIDA,
                    "FechaVenta", FieldValue.serverTimestamp()
            ));

            Map<String, Object> respuesta = new HashMap<>(doc.getData());
            respuesta.put("id", doc.getId());
            respuesta.put("Estado", VENDIDA);

            return respuesta;
        }).get();
    }

    // ============================================================
    // GENERAR LA CANTIDAD DE FICHAS SOLICITADA
    // ============================================================
    //
    // numFichas ya no es una variable global.
    //
    // Ejemplo:
    //
    // generarFichas(10)
    //
    // genera exactamente 10 fichas.
    //
    // ============================================================

    public List<String> generarFichas(
            int numFichas
    ) throws Exception {

        Set<String> fichasGeneradas =
                generarClavesUnicas(
                        numFichas
                );


        guardarFichas(
                fichasGeneradas
        );


        return new ArrayList<>(
                fichasGeneradas
        );
    }


    private Set<String> generarClavesUnicas(
            int numFichas
    ) {

        Set<String> fichas =
                new HashSet<>();


        while (
                fichas.size() < numFichas
        ) {

            String ficha =
                    generarClave();


            fichas.add(
                    ficha
            );
        }


        return fichas;
    }


    // ============================================================
    // GENERAR FICHA DE 6 DÍGITOS
    // ============================================================
    //
    // Rango:
    //
    // 100000 - 999999
    //
    // ============================================================

    private String generarClave() {

        int numero =
                100000
                + random.nextInt(900000);


        return String.valueOf(
                numero
        );
    }


    // ============================================================
    // GUARDAR EN FIRESTORE
    // ============================================================
    //
    // Se utilizan exactamente 500 escrituras,
    // por lo que cabe en un único WriteBatch.
    //
    // ============================================================

    private void guardarFichas(
            Set<String> fichas
    ) throws Exception {

        WriteBatch batch =
                db.batch();


        String nombreCarga =
                LocalDate.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd-MM-yyyy"
                                )
                        );


        for (
                String ficha : fichas
        ) {

            Map<String, Object> datos =
                    new HashMap<>();


            datos.put(
                    "Estado",
                    "NUEVA"
            );


            datos.put(
                    "FechaVenta",
                    ""
            );


            datos.put(
                    "Ficha",
                    ficha
            );


            datos.put(
                    "NombreCarga",
                    nombreCarga
            );


            /*
             * Utilizamos un ID automático de Firestore.
             *
             * Esto es importante porque tú no quieres
             * validar contra fichas históricas.
             */

            DocumentReference documento =
                    db.collection(COLLECTION).document();


            batch.set(
                    documento,
                    datos
            );
        }


        batch.commit().get();
    }

    // ============================================================
    // LIMPIAR FICHAS VENDIDAS
    // ============================================================
    //
    // Elimina de Firestore todos los documentos de ZonaDFichas
    // cuyo campo:
    //
    // Estado = "VENDIDA"
    //
    // Se procesan en bloques de máximo 500 documentos.
    //
    // Devuelve la cantidad total de documentos eliminados.
    // ============================================================

    public int limpiarFichasVendidas() throws Exception {

        int totalEliminadas = 0;

        while (true) {

            QuerySnapshot snapshot =
                    db
                            .collection(COLLECTION)
                            .whereEqualTo(
                                    "Estado",
                                    "VENDIDA"
                            )
                            .limit(500)
                            .get()
                            .get();


            // Ya no existen fichas vendidas
            if (snapshot.isEmpty()) {
                break;
            }


            WriteBatch batch =
                    db.batch();


            for (
                    DocumentSnapshot documento :
                    snapshot.getDocuments()
            ) {

                batch.delete(
                        documento.getReference()
                );

                totalEliminadas++;
            }


            batch.commit().get();
        }


        return totalEliminadas;
    }

    // ============================================================
    // CONSULTAR RESUMEN DE FICHAS
    // ============================================================

    public Map<String, Long> consultarResumenFichas()
            throws Exception {

        long nuevas =
                contarFichasNuevas();

        long vendidas =
                contarFichasVendidas();


        Map<String, Long> respuesta =
                new HashMap<>();


        respuesta.put(
                "Nuevas",
                nuevas
        );

        respuesta.put(
                "Vendidas",
                vendidas
        );


        return respuesta;
    }

    private long contarFichasNuevas()
        throws Exception {

        QuerySnapshot snapshot =
                db
                        .collection(COLLECTION)
                        .whereEqualTo("Estado", NUEVA)
                        .get()
                        .get();

        return snapshot.size();
    }

    private long contarFichasVendidas()
        throws Exception {

        QuerySnapshot snapshot =
                db
                        .collection(COLLECTION)
                        .whereEqualTo("Estado", VENDIDA)
                        .get()
                        .get();

        return snapshot.size();
    }
}
