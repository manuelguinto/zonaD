package com.zonad.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

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
}
