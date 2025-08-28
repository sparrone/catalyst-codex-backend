package com.separrone.awakeningbackend.service;

import com.google.cloud.storage.*;
import com.google.api.gax.paging.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.gcs.backup-enabled", havingValue = "true", matchIfMissing = false)
public class DatabaseBackupService {

    private final String bucketName;
    private final String databasePath;
    private final Storage storage;
    
    private static final String BACKUP_PREFIX = "awakening-backup-";
    private static final String BACKUP_SUFFIX = ".db";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public DatabaseBackupService(
            @Value("${app.gcs.bucket-name}") String bucketName,
            @Value("${spring.datasource.url}") String datasourceUrl) {
        
        this.bucketName = bucketName;
        this.databasePath = extractDatabasePath(datasourceUrl);
        this.storage = StorageOptions.getDefaultInstance().getService();
        
        log.info("DatabaseBackupService initialized - Bucket: {}, DB Path: {}", bucketName, databasePath);
    }

    /**
     * Restore database from backup when application starts (production only)
     * Always restore from latest backup to ensure session data persists across cold starts
     * Non-blocking: Application continues startup even if backup restoration fails
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - attempting to restore from latest backup...");
        
        try {
            boolean restored = restoreFromLatestBackup();
            if (!restored) {
                log.info("No timestamped backup found, trying initial backup...");
                restored = restoreFromBackup("awakening-backup-initial.db");
                if (!restored) {
                    log.warn("No backup found to restore. Application will continue with existing/empty database.");
                } else {
                    log.info("Successfully restored from initial backup");
                }
            } else {
                log.info("Successfully restored from latest backup");
            }
        } catch (Exception e) {
            log.error("Backup restoration failed, but application will continue startup", e);
        }
    }

    /**
     * Scheduled backup job - runs every 6 hours by default
     */
    @Scheduled(fixedRateString = "${app.gcs.backup-interval-hours:6}0000", initialDelay = 300000) // 5 min initial delay
    public void scheduledBackup() {
        try {
            log.info("Starting scheduled database backup...");
            createBackup();
            cleanupOldBackups();
            log.info("Scheduled backup completed successfully");
        } catch (Exception e) {
            log.error("Scheduled backup failed", e);
        }
    }

    /**
     * Manual backup method that can be called programmatically
     */
    public String createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupFileName = BACKUP_PREFIX + timestamp + BACKUP_SUFFIX;
        
        log.info("Creating backup: {}", backupFileName);
        
        // Verify source database exists
        Path dbPath = Paths.get(databasePath);
        if (!Files.exists(dbPath)) {
            throw new IOException("Database file not found: " + databasePath);
        }
        
        // Create temporary backup file
        Path tempBackupPath = createTemporaryBackup(dbPath, timestamp);
        
        try {
            // Upload to Google Cloud Storage
            uploadToGCS(tempBackupPath, backupFileName);
            
            log.info("Backup uploaded successfully: gs://{}/{}", bucketName, backupFileName);
            return backupFileName;
            
        } finally {
            // Clean up temporary file
            try {
                Files.deleteIfExists(tempBackupPath);
            } catch (IOException e) {
                log.warn("Failed to delete temporary backup file: {}", tempBackupPath, e);
            }
        }
    }

    /**
     * Restore database from the latest backup
     */
    public boolean restoreFromLatestBackup() {
        try {
            log.info("Attempting to restore from latest backup...");
            
            String latestBackup = findLatestBackup();
            if (latestBackup == null) {
                log.warn("No backups found in bucket: {}", bucketName);
                return false;
            }
            
            return restoreFromBackup(latestBackup);
            
        } catch (Exception e) {
            log.error("Failed to restore from latest backup", e);
            return false;
        }
    }

    /**
     * Restore database from a specific backup
     */
    public boolean restoreFromBackup(String backupFileName) {
        try {
            log.info("Restoring database from backup: {}", backupFileName);
            
            // Download backup from GCS
            Path tempRestorePath = downloadFromGCS(backupFileName);
            
            try {
                // Ensure database directory exists
                Path dbPath = Paths.get(databasePath);
                Files.createDirectories(dbPath.getParent());
                
                // Replace current database with backup
                Files.copy(tempRestorePath, dbPath, StandardCopyOption.REPLACE_EXISTING);
                
                log.info("Database restored successfully from: {}", backupFileName);
                return true;
                
            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempRestorePath);
            }
            
        } catch (Exception e) {
            log.error("Failed to restore from backup: {}", backupFileName, e);
            return false;
        }
    }

    private Path createTemporaryBackup(Path dbPath, String timestamp) throws IOException {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempBackupPath = tempDir.resolve(BACKUP_PREFIX + timestamp + BACKUP_SUFFIX);
        
        // Use file copy for SQLite backup (simple and reliable)
        Files.copy(dbPath, tempBackupPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.debug("Temporary backup created: {}", tempBackupPath);
        return tempBackupPath;
    }

    private void uploadToGCS(Path filePath, String objectName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/x-sqlite3")
                .build();
        
        byte[] fileContent = Files.readAllBytes(filePath);
        storage.create(blobInfo, fileContent);
        
        log.debug("File uploaded to GCS: gs://{}/{}", bucketName, objectName);
    }

    private Path downloadFromGCS(String objectName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);
        
        if (blob == null) {
            throw new IOException("Backup file not found in GCS: " + objectName);
        }
        
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempRestorePath = tempDir.resolve("restore-" + objectName);
        
        Files.write(tempRestorePath, blob.getContent());
        
        log.debug("File downloaded from GCS: {}", tempRestorePath);
        return tempRestorePath;
    }

    private String findLatestBackup() {
        try {
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(BACKUP_PREFIX));
            
            return blobs.streamAll()
                    .filter(blob -> blob.getName().startsWith(BACKUP_PREFIX) && blob.getName().endsWith(BACKUP_SUFFIX))
                    .sorted((a, b) -> b.getCreateTimeOffsetDateTime().compareTo(a.getCreateTimeOffsetDateTime()))
                    .findFirst()
                    .map(Blob::getName)
                    .orElse(null);
                    
        } catch (Exception e) {
            log.error("Error finding latest backup", e);
            return null;
        }
    }

    private void cleanupOldBackups() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(BACKUP_PREFIX));
            
            List<Blob> oldBackups = blobs.streamAll()
                    .filter(blob -> blob.getName().startsWith(BACKUP_PREFIX))
                    .filter(blob -> blob.getCreateTimeOffsetDateTime().toLocalDateTime().isBefore(cutoffTime))
                    .collect(Collectors.toList());
            
            for (Blob oldBackup : oldBackups) {
                storage.delete(oldBackup.getBlobId());
                log.info("Deleted old backup: {}", oldBackup.getName());
            }
            
            if (oldBackups.isEmpty()) {
                log.debug("No old backups to clean up");
            } else {
                log.info("Cleaned up {} old backups", oldBackups.size());
            }
            
        } catch (Exception e) {
            log.error("Error cleaning up old backups", e);
        }
    }

    private String extractDatabasePath(String datasourceUrl) {
        // Extract file path from JDBC URL (e.g., "jdbc:sqlite:/app/data/awakening-prod.db")
        if (datasourceUrl.startsWith("jdbc:sqlite:")) {
            return datasourceUrl.substring("jdbc:sqlite:".length());
        }
        throw new IllegalArgumentException("Invalid SQLite datasource URL: " + datasourceUrl);
    }
}