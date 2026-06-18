package com.separrone.awakeningbackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.separrone.awakeningbackend.model.FirestoreUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreUserRepository {

    private static final String COLLECTION_NAME = "users";

    @Autowired
    private Firestore firestore;

    public Optional<FirestoreUser> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToUser(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching user", e);
        }
    }

    public Optional<FirestoreUser> findByUsername(String username) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("username", username)
                    .limit(1);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (!querySnapshot.getDocuments().isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(documentToUser(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching user by username", e);
        }
    }

    public Optional<FirestoreUser> findByEmail(String email) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("email", email)
                    .limit(1);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (!querySnapshot.getDocuments().isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(documentToUser(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching user by email", e);
        }
    }

    public FirestoreUser save(FirestoreUser user) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(user.getId());
            Map<String, Object> userData = userToMap(user);
            ApiFuture<WriteResult> future = docRef.set(userData);
            future.get(); // Wait for completion
            return user;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    public void delete(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    private FirestoreUser documentToUser(DocumentSnapshot document) {
        FirestoreUser user = new FirestoreUser();
        user.setId(document.getId());
        user.setUsername(document.getString("username"));
        user.setEmail(document.getString("email"));
        user.setEnabled(document.getBoolean("enabled"));
        
        // Convert Timestamp to LocalDateTime
        com.google.cloud.Timestamp timestamp = document.getTimestamp("createdAt");
        if (timestamp != null) {
            user.setCreatedAt(LocalDateTime.ofEpochSecond(
                timestamp.getSeconds(), 
                timestamp.getNanos(), 
                ZoneOffset.UTC
            ));
        }
        
        return user;
    }

    private Map<String, Object> userToMap(FirestoreUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("enabled", user.isEnabled());
        
        if (user.getCreatedAt() != null) {
            userData.put("createdAt", com.google.cloud.Timestamp.of(
                java.util.Date.from(user.getCreatedAt().toInstant(ZoneOffset.UTC))
            ));
        }
        
        return userData;
    }
}