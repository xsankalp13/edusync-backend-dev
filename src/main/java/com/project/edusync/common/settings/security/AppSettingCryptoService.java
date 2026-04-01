package com.project.edusync.common.settings.security;

import com.project.edusync.common.exception.EdusyncException;
import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppSettingCryptoService {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    private final StringEncryptor jasyptEncryptor;

    @Value("${JASYPT_ENCRYPTOR_PASSWORD:${APP_SETTINGS_ENCRYPTION_KEY:}}")
    private String jasyptPassword;

    public String encryptForStorage(String rawValue) {
        ensureConfigured();
        String source = rawValue == null ? "" : rawValue;
        return ENC_PREFIX + jasyptEncryptor.encrypt(source) + ENC_SUFFIX;
    }

    public String decryptFromStorage(String storedValue) {
        ensureConfigured();
        if (storedValue == null) {
            return "";
        }
        if (!isWrapped(storedValue)) {
            throw new EdusyncException("Encrypted setting is stored in invalid format.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String cipher = storedValue.substring(ENC_PREFIX.length(), storedValue.length() - ENC_SUFFIX.length());
        return jasyptEncryptor.decrypt(cipher);
    }

    public boolean isWrapped(String value) {
        return value != null && value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    private void ensureConfigured() {
        if (jasyptPassword == null || jasyptPassword.isBlank()) {
            throw new EdusyncException(
                    "Encryption key is not configured. Set JASYPT_ENCRYPTOR_PASSWORD (or APP_SETTINGS_ENCRYPTION_KEY for backward compatibility).",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}



