package com.separrone.awakeningbackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.separrone.awakeningbackend.model.FirestoreUserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreUserSettingsRepository {

    private static final String COLLECTION_NAME = "userSettings";

    @Autowired
    private Firestore firestore;

    public Optional<FirestoreUserSettings> findByUserId(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToUserSettings(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching user settings", e);
        }
    }

    public FirestoreUserSettings save(FirestoreUserSettings userSettings) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userSettings.getUserId());
            Map<String, Object> settingsData = userSettingsToMap(userSettings);
            ApiFuture<WriteResult> future = docRef.set(settingsData);
            future.get(); // Wait for completion
            return userSettings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving user settings", e);
        }
    }

    public void delete(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting user settings", e);
        }
    }

    private FirestoreUserSettings documentToUserSettings(DocumentSnapshot document) {
        FirestoreUserSettings settings = new FirestoreUserSettings();
        settings.setUserId(document.getId());
        settings.setTheme(document.getString("theme"));
        settings.setEmailNotifications(document.getBoolean("emailNotifications"));
        settings.setTimezone(document.getString("timezone"));
        settings.setLanguage(document.getString("language"));
        return settings;
    }

    private Map<String, Object> userSettingsToMap(FirestoreUserSettings settings) {
        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put("theme", settings.getTheme());
        settingsData.put("emailNotifications", settings.isEmailNotifications());
        settingsData.put("timezone", settings.getTimezone());
        settingsData.put("language", settings.getLanguage());
        return settingsData;
    }
}