package com.backend.backend.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA que cifra campos {@code String} con AES-256-GCM al persistirlos y los descifra al
 * leerlos. Delega en {@link EncryptionService} obtenido del contexto de Spring.
 *
 * <p>Uso en entidades: {@code @Convert(converter = StringEncryptedConverter.class)}
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
