package com.zonad.api.service;

import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FichaService {

    private static final String COLLECTION = "ZonaDFichas";
    private static final String NUEVA = "NUEVA";
    private static final String VENDIDA = "VENDIDA";

    private final Firestore db;

    public FichaService(Firestore db) {
        this.db = db;
    }

    public Map<String, Object> venderFicha()
            throws ExecutionException, InterruptedException {

        return db.runTransaction(transaction -> {

            Query query = db.collection(COLLECTION)
                    .whereEqualTo("estatus", NUEVA)
                    .limit(1);

            QuerySnapshot snapshot = transaction.get(query).get();
            List<QueryDocumentSnapshot> docs = snapshot.getDocuments();

            if (docs.isEmpty()) {
                return null;
            }

            QueryDocumentSnapshot doc = docs.get(0);
            DocumentReference ref = doc.getReference();

            transaction.update(ref, Map.of(
                    "estatus", VENDIDA,
                    "fechaVenta", FieldValue.serverTimestamp()
            ));

            Map<String, Object> respuesta = new HashMap<>(doc.getData());
            respuesta.put("id", doc.getId());
            respuesta.put("estatus", VENDIDA);

            return respuesta;
        }).get();
    }
}
