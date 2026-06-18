package com.separrone.awakeningbackend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreCategory {
    
    private String id;
    private String name;
    private String description;
    
    // Constructor for creating new categories
    public FirestoreCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }
}