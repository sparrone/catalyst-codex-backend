package com.separrone.awakeningbackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.separrone.awakeningbackend.model.FirestoreCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class FirestoreCategoryRepository {

    private static final String COLLECTION_NAME = "categories";

    @Autowired
    private Firestore firestore;

    public Optional<FirestoreCategory> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToCategory(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching category", e);
        }
    }

    public List<FirestoreCategory> findAll() {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .orderBy("name", Query.Direction.ASCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToCategory)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching all categories", e);
        }
    }

    public Optional<FirestoreCategory> findByName(String name) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("name", name)
                    .limit(1);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (!querySnapshot.getDocuments().isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(documentToCategory(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching category by name", e);
        }
    }

    public FirestoreCategory save(FirestoreCategory category) {
        try {
            if (category.getId() == null) {
                // Create new document with auto-generated ID
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document();
                category.setId(docRef.getId());
            }
            
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(category.getId());
            Map<String, Object> categoryData = categoryToMap(category);
            ApiFuture<WriteResult> future = docRef.set(categoryData);
            future.get(); // Wait for completion
            return category;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving category", e);
        }
    }

    public void delete(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting category", e);
        }
    }

    private FirestoreCategory documentToCategory(DocumentSnapshot document) {
        FirestoreCategory category = new FirestoreCategory();
        category.setId(document.getId());
        category.setName(document.getString("name"));
        category.setDescription(document.getString("description"));
        return category;
    }

    private Map<String, Object> categoryToMap(FirestoreCategory category) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", category.getName());
        categoryData.put("description", category.getDescription());
        return categoryData;
    }
}