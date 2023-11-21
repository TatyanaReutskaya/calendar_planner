package org.tvr.YourCalendar.services;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.tvr.YourCalendar.exception.UrlFromMailException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncodeDecodeServise {
    private Key key;
    private Cipher cipher;
    private IvParameterSpec ivSpec;
    private final String transformation = "AES/CBC/PKCS5Padding";
    @PostConstruct
    @SneakyThrows
    private void init() {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        key = keygen.generateKey();
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        ivSpec = new IvParameterSpec(iv);
        cipher = Cipher.getInstance(transformation);
    }
    @SneakyThrows
    public String encode(String text) {
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(text.getBytes("utf8"));
        Base64.Encoder encoder = Base64.getUrlEncoder();
        return encoder.encodeToString(encrypted);
    }
    @SneakyThrows
    public String decode(String text){
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        Base64.Decoder decoder = Base64.getUrlDecoder();
        byte[] decryptedBytes = new byte[0];
        try {
            decryptedBytes = cipher.doFinal(decoder.decode(text));
        } catch (IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e) {
            throw new UrlFromMailException("Извините, ссылка недоступна :(");
        }
        return new String(decryptedBytes);
    }
}
