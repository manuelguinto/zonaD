package com.zonad.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {

        return FirestoreOptions.newBuilder()
                .setProjectId("zona-d")
                .setDatabaseId("zona-d")
                .build()
                .getService();
    }
}