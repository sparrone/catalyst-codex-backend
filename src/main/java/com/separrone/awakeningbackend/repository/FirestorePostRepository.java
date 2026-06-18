package com.separrone.awakeningbackend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.separrone.awakeningbackend.model.FirestorePost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class FirestorePostRepository {

    private static final String COLLECTION_NAME = "posts";

    @Autowired
    private Firestore firestore;

    public Optional<FirestorePost> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.of(documentToPost(document));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching post", e);
        }
    }

    public List<FirestorePost> findByThreadId(String threadId) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("threadId", threadId)
                    .orderBy("createdAt", Query.Direction.ASCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToPost)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching posts by thread", e);
        }
    }

    public List<FirestorePost> findByAuthorId(String authorId) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("authorId", authorId)
                    .orderBy("createdAt", Query.Direction.DESCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToPost)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching posts by author", e);
        }
    }

    public FirestorePost save(FirestorePost post) {
        try {
            if (post.getId() == null) {
                // Create new document with auto-generated ID
                DocumentReference docRef = firestore.collection(COLLECTION_NAME).document();
                post.setId(docRef.getId());
            }
            
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(post.getId());
            Map<String, Object> postData = postToMap(post);
            ApiFuture<WriteResult> future = docRef.set(postData);
            future.get(); // Wait for completion
            return post;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving post", e);
        }
    }

    public void delete(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting post", e);
        }
    }

    private FirestorePost documentToPost(DocumentSnapshot document) {
        FirestorePost post = new FirestorePost();
        post.setId(document.getId());
        post.setContent(document.getString("content"));
        post.setThreadId(document.getString("threadId"));
        post.setAuthorId(document.getString("authorId"));
        
        // Convert Timestamp to LocalDateTime
        com.google.cloud.Timestamp timestamp = document.getTimestamp("createdAt");
        if (timestamp != null) {
            post.setCreatedAt(LocalDateTime.ofEpochSecond(
                timestamp.getSeconds(), 
                timestamp.getNanos(), 
                ZoneOffset.UTC
            ));
        }
        
        return post;
    }

    private Map<String, Object> postToMap(FirestorePost post) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("content", post.getContent());
        postData.put("threadId", post.getThreadId());
        postData.put("authorId", post.getAuthorId());
        
        if (post.getCreatedAt() != null) {
            postData.put("createdAt", com.google.cloud.Timestamp.of(
                java.util.Date.from(post.getCreatedAt().toInstant(ZoneOffset.UTC))
            ));
        }
        
        return postData;
    }
}