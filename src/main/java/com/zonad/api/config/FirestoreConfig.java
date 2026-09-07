package com.zonad.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {

        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "No se configuró la variable GOOGLE_CLOUD_PROJECT"
            );
        }

        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId("zona-d")
                .build()
                .getService();
    }
}