package com.project.edusync.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.util.StringUtils;

/**
 * Encrypts/decrypts sensitive DB columns using the same Jasypt password used elsewhere in the app.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (!StringUtils.hasText(attribute)) {
            return attribute;
        }

        String key = resolveEncryptionKey();
        if (!StringUtils.hasText(key)) {
            // Keep backward compatibility in environments where encryption key is not configured yet.
            return attribute;
        }

        if (isWrapped(attribute)) {
            return attribute;
        }

        return ENC_PREFIX + encryptor(key).encrypt(attribute) + ENC_SUFFIX;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!isWrapped(dbData)) {
            return dbData;
        }

        String key = resolveEncryptionKey();
        if (!StringUtils.hasText(key)) {
            return dbData;
        }

        String cipher = dbData.substring(ENC_PREFIX.length(), dbData.length() - ENC_SUFFIX.length());
        return encryptor(key).decrypt(cipher);
    }

    private boolean isWrapped(String value) {
        return value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    private String resolveEncryptionKey() {
        String key = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (!StringUtils.hasText(key)) {
            key = System.getenv("APP_SETTINGS_ENCRYPTION_KEY");
        }
        if (!StringUtils.hasText(key)) {
            key = System.getProperty("JASYPT_ENCRYPTOR_PASSWORD");
        }
        if (!StringUtils.hasText(key)) {
            key = System.getProperty("APP_SETTINGS_ENCRYPTION_KEY");
        }
        return key;
    }

    private BasicTextEncryptor encryptor(String key) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(key);
        return encryptor;
    }
}

