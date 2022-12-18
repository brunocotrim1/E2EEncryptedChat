package org.psd.CloudPSD.service;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.psd.CloudPSD.models.Message;
import org.psd.CloudPSD.models.SSETable;
import org.psd.CloudPSD.models.network.MessageDTO;
import org.psd.CloudPSD.models.network.SseDTO;
import org.psd.CloudPSD.repositories.IMessageRepository;
import org.psd.CloudPSD.repositories.ISSERepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.transaction.Transactional;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class SSEService {
    private final ISSERepository sseRepository;
    private final IMessageRepository messageRepository;
    public SSEService(ISSERepository sseRepository, IMessageRepository messageRepository) {
        this.sseRepository = sseRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void deleteAll(String username) {
        sseRepository.deleteAllByUsername(username);
    }

    @Transactional
    public ResponseEntity<?> update(SseDTO sseDTO,String username) {
        try {
            SSETable sseTable = SSETable.builder().username(username)
                    .indexLabel(sseDTO.getValue1())
                    .indexValue(Base64.getDecoder().decode(sseDTO.getValue2()))
                    .iv(sseDTO.getIv())
                    .build();
            sseRepository.save(sseTable);
            log.info("SSE update successful: "+sseTable);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> search(SseDTO sseDTO, String username) throws NoSuchAlgorithmException, InvalidKeyException {
        List<SSETable> sseTables = sseRepository.findByUsername(username);
        HashMap<String, Pair<byte[],byte[]>> indexMap = listToMap(sseTables);
        SecretKey prfKey = decodeToKey(sseDTO.getValue1());
        SecretKey prfKey2 = decodeToKey(sseDTO.getValue2());

        List<MessageDTO> indexesToReturn = new ArrayList<>();
        Integer c = new Integer(0);
        while(true){
            List<MessageDTO> result = new ArrayList<>();
            SecretKey indexLabel = pseudoRandomFunction(prfKey,new byte[]{c.byteValue()});
            Pair<byte[],byte[]> indexValueAndIV = indexMap.get(Base64.getEncoder().encodeToString(indexLabel.getEncoded()));
            //pairKey e o indexValue encriptado e o value e o IV
            if(indexValueAndIV ==null){
                log.info("IndexValue reached an end");
                break;
            }
            byte [] indexValue = indexValueAndIV.getKey();
            try {
                Message message = decrypt(indexValue,prfKey2,indexValueAndIV.getValue());
                if(message != null){
                    result.add(message.toDTO());
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            if(result.size() == 0){
                break;
            }
            indexesToReturn.addAll(result);
            c++;
        }
        log.info("SSE search successful: "+indexesToReturn);
        return ResponseEntity.ok(indexesToReturn);
    }

    public static SecretKey pseudoRandomFunction(SecretKey key, byte[] input) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] macApplied =  mac.doFinal(input);
        return new SecretKeySpec(macApplied,0,macApplied.length,"AES");
    }

    private HashMap<String, Pair<byte[],byte[]>> listToMap(List<SSETable> sseTables){
        HashMap<String,Pair<byte[],byte[]>> map = new HashMap<>();
        for(SSETable sseTable:sseTables){
            map.put(sseTable.getIndexLabel(),new Pair<>(sseTable.getIndexValue(),sseTable.getIv()));
        }
        return map;
    }

    private SecretKey decodeToKey(String encodedKey){
        return new SecretKeySpec(Base64.getDecoder().decode(encodedKey), "AES/GCM/NoPadding");
    }

    public Message decrypt(byte[] cipherText, SecretKey key, byte[]iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding","BC");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plainText = cipher.doFinal(cipherText);
        String messageId = new String(plainText);
        return messageRepository.findById(messageId).orElse(null);
    }

}
