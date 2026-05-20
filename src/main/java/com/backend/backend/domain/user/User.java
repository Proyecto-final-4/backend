package com.backend.backend.domain.user;

import com.backend.backend.shared.BaseEntity;
import com.backend.backend.shared.crypto.StringEncryptedConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** AES-256-GCM encrypted value. Equality lookups use {@code email_hmac}. */
    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = StringEncryptedConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    /** HMAC-SHA256 of the normalized email. Enables deterministic lookups on encrypted email. */
    @Column(name = "email_hmac", nullable = false, unique = true, length = 64)
    private String emailHmac;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailHmac() {
        return emailHmac;
    }

    public void setEmailHmac(String emailHmac) {
        this.emailHmac = emailHmac;
    }
}
