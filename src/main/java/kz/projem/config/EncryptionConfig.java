package kz.projem.config;

import jakarta.annotation.PostConstruct;
import kz.projem.security.EncryptedStringConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${app.security.encryption-key}")
    private String encryptionKey;

    @PostConstruct
    public void init() {
        EncryptedStringConverter.init(encryptionKey);
    }
}
