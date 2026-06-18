package com.separrone.awakeningbackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.separrone.awakeningbackend.model.FirestoreThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class FirestoreThreadRepository {

    private static final String COLLECTION_NAME = "threads";

    @Autowired
    private Firestore firestore;

    public Optional<FirestoreThread> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToThread(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching thread", e);
        }
    }

    public List<FirestoreThread> findByCategoryId(String categoryId) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToThread)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching threads by category", e);
        }
    }

    public List<FirestoreThread> findByAuthorId(String authorId) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("authorId", authorId)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToThread)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching threads by author", e);
        }
    }

    public List<FirestoreThread> findAll() {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToThread)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching all threads", e);
        }
    }

    public FirestoreThread save(FirestoreThread thread) {
        try {
            if (thread.getId() == null) {
                // Create new document with auto-generated ID
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document();
                thread.setId(docRef.getId());
            }
            
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(thread.getId());
            Map<String, Object> threadData = threadToMap(thread);
            ApiFuture<WriteResult> future = docRef.set(threadData);
            future.get(); // Wait for completion
            return thread;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving thread", e);
        }
    }

    public void delete(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting thread", e);
        }
    }

    private FirestoreThread documentToThread(DocumentSnapshot document) {
        FirestoreThread thread = new FirestoreThread();
        thread.setId(document.getId());
        thread.setTitle(document.getString("title"));
        thread.setCategoryId(document.getString("categoryId"));
        thread.setAuthorId(document.getString("authorId"));
        
        // Convert Timestamp to LocalDateTime
        com.google.cloud.Timestamp timestamp = document.getTimestamp("createdAt");
        if (timestamp != null) {
            thread.setCreatedAt(LocalDateTime.ofEpochSecond(
                timestamp.getSeconds(), 
                timestamp.getNanos(), 
                ZoneOffset.UTC
            ));
        }
        
        return thread;
    }

    private Map<String, Object> threadToMap(FirestoreThread thread) {
        Map<String, Object> threadData = new HashMap<>();
        threadData.put("title", thread.getTitle());
        threadData.put("categoryId", thread.getCategoryId());
        threadData.put("authorId", thread.getAuthorId());
        
        if (thread.getCreatedAt() != null) {
            threadData.put("createdAt", com.google.cloud.Timestamp.of(
                java.util.Date.from(thread.getCreatedAt().toInstant(ZoneOffset.UTC))
            ));
        }
        
        return threadData;
    }
}