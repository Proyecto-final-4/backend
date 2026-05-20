package com.backend.backend.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that encrypts {@code String} fields with AES-256-GCM on persist and decrypts on
 * load. Delegates to {@link EncryptionService} obtained from the Spring context.
 *
 * <p>Entity usage: {@code @Convert(converter = StringEncryptedConverter.class)}
 */
@Converter
public class StringEncryptedConverter implements AttributeConverter<String, String> {

    private EncryptionService encryptionService() {
        return ApplicationContextProvider.getContext().getBean(EncryptionService.class);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService().decrypt(dbData);
    }
}
