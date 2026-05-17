package com.aick.mmp.central.migration;

import com.aick.mmp.shared.util.AESEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class V20260517__EncryptCameraPasswords extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        // Read the camera credential key from environment variable or use dev default
        String cameraKey = System.getenv("CAMERA_CREDENTIAL_KEY");
        if (cameraKey == null || cameraKey.isEmpty()) {
            cameraKey = "mmp-camera-encryption-key!!";
            log.warn("CAMERA_CREDENTIAL_KEY not set, using dev default. " +
                     "Set this env var in production before deploying this migration.");
        }

        // Use a dummy main key — we only need camera key for this migration
        AESEncryptionUtil encryptionUtil = new AESEncryptionUtil(
            "dummy-main-key-not-used-here",
            cameraKey
        );

        // Find all cameras with non-null, non-empty passwords
        var rows = new java.util.ArrayList<Map.Entry<Long, String>>();
        try (Statement stmt = context.getConnection().createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT id, password FROM cameras WHERE password IS NOT NULL AND password != ''")) {
                while (rs.next()) {
                    rows.add(Map.entry(
                        rs.getLong("id"),
                        rs.getString("password")
                    ));
                }
            }
        }

        if (rows.isEmpty()) {
            log.info("No camera passwords to migrate");
            return;
        }

        int encryptedCount = 0;
        int skippedCount = 0;

        for (var row : rows) {
            Long id = row.getKey();
            String password = row.getValue();

            // Skip if already encrypted (Base64 decode length >= 28 bytes)
            if (isAlreadyEncrypted(password)) {
                skippedCount++;
                continue;
            }

            // Encrypt the plaintext password
            String encrypted = encryptionUtil.encryptCameraPassword(password);

            try (var pstmt = context.getConnection().prepareStatement(
                    "UPDATE cameras SET password = ? WHERE id = ?")) {
                pstmt.setString(1, encrypted);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
            encryptedCount++;
            log.debug("Encrypted password for camera: {}", id);
        }

        log.info("Camera password migration complete: {} encrypted, {} already encrypted, {} total",
                encryptedCount, skippedCount, rows.size());
    }

    private boolean isAlreadyEncrypted(String value) {
        if (value == null || value.length() < 28) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= 28;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
