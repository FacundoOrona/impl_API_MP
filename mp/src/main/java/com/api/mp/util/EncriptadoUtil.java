package com.api.mp.util;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncriptadoUtil {
    
    @Value("${algoritmoEncriptar}")
    private String algoritmoEncriptar;

    @Value("${SecretKeyEncriptar}")
    private String secretKeyEncriptar;

    public String encriptar(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secretKeyEncriptar.getBytes(), algoritmoEncriptar);
            Cipher cipher = Cipher.getInstance(algoritmoEncriptar);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el token", e);
        }
    }

}
