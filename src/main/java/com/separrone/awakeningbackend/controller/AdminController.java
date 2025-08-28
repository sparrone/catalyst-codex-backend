package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.service.DatabaseBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@ConditionalOnBean(DatabaseBackupService.class)
public class AdminController {

    private final DatabaseBackupService backupService;

    @Autowired
    public AdminController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Manually trigger a database backup
     * Only accessible by authenticated admin users
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBackup() {
        try {
            log.info("Manual backup requested");
            String backupFileName = backupService.createBackup();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Backup created successfully",
                "backupFile", backupFileName
            ));
            
        } catch (Exception e) {
            log.error("Manual backup failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Backup failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Restore database from latest backup
     * Only accessible by authenticated admin users
     */
    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreFromBackup() {
        try {
            log.info("Manual restore requested");
            boolean restored = backupService.restoreFromLatestBackup();
            
            if (restored) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Database restored successfully from latest backup"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No backups available to restore"
                ));
            }
            
        } catch (Exception e) {
            log.error("Manual restore failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Restore failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for backup service
     */
    @GetMapping("/backup/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getBackupStatus() {
        return ResponseEntity.ok(Map.of(
            "backupServiceEnabled", true,
            "message", "Database backup service is running"
        ));
    }
}