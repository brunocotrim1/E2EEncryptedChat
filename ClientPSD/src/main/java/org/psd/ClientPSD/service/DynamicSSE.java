package org.psd.ClientPSD.service;

import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.configuration.AuthenticationSetup;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.SseDTO;
import org.psd.ClientPSD.model.network.MessageDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
@Service
@Slf4j
public class DynamicSSE {

    private final RestTemplate restTemplate;

    public static HashMap<String,Integer> counterDict = new HashMap<>();
    private final Properties properties;
    private final AuthenticationSetup authenticationSetup;
    public DynamicSSE(RestTemplate restTemplate, Properties properties, AuthenticationSetup authenticationSetup) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.authenticationSetup = authenticationSetup;
        reset();
    }


    public void update(String word, String id, SecretKey signingKey) throws Exception {
        SecretKey prfKey = pseudoRandomFunction(signingKey,new String(word+"1").getBytes());
        SecretKey prfKey2 = pseudoRandomFunction(signingKey,new String(word+"2").getBytes());
        Integer c = counterDict.getOrDefault(word,0);
        SecretKey indexLabel = pseudoRandomFunction(prfKey,new byte[]{c.byteValue()});
        IvParameterSpec iv = generateIv();
        byte[] indexValue = encrypt(id.getBytes(),prfKey2,iv);
        // indexMap.put(Base64.getEncoder().encodeToString(indexLabel.getEncoded()),indexValue);
        SseDTO sseDTO = SseDTO.builder().value1(Base64.getEncoder().encodeToString(indexLabel.getEncoded()))
                .value2(Base64.getEncoder().encodeToString(indexValue)).iv(iv.getIV()).build();
        sendUpdateToServer(sseDTO);
        c ++;
        counterDict.put(word,c);
    }

    public List<MessageDTO> search(String word, SecretKey signingKey){
        try {
            SecretKey prfKey = pseudoRandomFunction(signingKey, new String(word + "1").getBytes());
            SecretKey prfKey2 = pseudoRandomFunction(signingKey, new String(word + "2").getBytes());
            SseDTO sseDTO = SseDTO.builder().value1(Base64.getEncoder().encodeToString(prfKey.getEncoded()))
                    .value2(Base64.getEncoder().encodeToString(prfKey2.getEncoded())).build();
            return requestSearch(sseDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static SecretKey generateKey(){
        Long num = new Long(55);
        byte [] seed = {num.byteValue()};
        SecureRandom rnd = new SecureRandom(seed);
        byte[] data = new byte[32]; // 16 * 8 = 128 bit
        rnd.nextBytes(data);
        BigInteger bigInt = new BigInteger(data);
        return new SecretKeySpec(bigInt.toByteArray(), 0,bigInt.toByteArray().length, "HmacSHA256");
    }

    public static SecretKey pseudoRandomFunction(SecretKey key, byte[] input) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] macApplied =  mac.doFinal(input);
        return new SecretKeySpec(macApplied,0,macApplied.length,"AES");
    }


    public static byte[] encrypt(byte[] data,SecretKey key,IvParameterSpec ivspec) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key,ivspec);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
        return c.doFinal(data);
    }
    private void reset(){
        try {
            restTemplate.exchange(properties.getCloudAddress() + "/sse/reset",
                    HttpMethod.POST, authenticationSetup.getHeader(), String.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    private void sendUpdateToServer(SseDTO sseDTO){
        try {
            ResponseEntity<?> response = restTemplate.exchange(properties.getCloudAddress() + "/sse/update",
                    HttpMethod.POST, authenticationSetup.getHeader(sseDTO), String.class);
            log.info("Sent SSE update to server: " + sseDTO);
        } catch (Exception exception) {
            log.info("Failed to send SSE update to server: " + sseDTO);
        }
    }

    private List<MessageDTO> requestSearch(SseDTO sseDTO){
        try {
            ResponseEntity<List<MessageDTO>> response = restTemplate.exchange(properties.getCloudAddress() + "/sse/search",
                    HttpMethod.POST, authenticationSetup.getHeader(sseDTO),  new ParameterizedTypeReference<List<MessageDTO>>() {});
            if(response.getBody() == null)
                return new ArrayList<>();
            if(response.getBody().size() == 0)
                return new ArrayList<>();
            log.info("Received SSE search response from server: " + response.getBody());
            return response.getBody();

        } catch (Exception exception) {
            log.info("Failed to receive SSE search response from server: " + sseDTO);
            return new ArrayList<>();
        }
    }
    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}
