package org.psd.ClientPSD.service;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.configuration.AuthenticationSetup;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.Group;
import org.psd.ClientPSD.model.network.ABEKeySharing;
import org.psd.ClientPSD.model.network.CreateGroup;
import org.psd.ClientPSD.model.network.MessageDTO;
import org.psd.ClientPSD.model.network.MessageUI;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupService {

    private final RestTemplate restTemplate;
    private final Properties properties;
    private final AuthenticationSetup authenticationSetup;
    private HashMap<String, Group> groups= new HashMap<>();
    private KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
    private final DynamicSSE dynamicSSE;


    public GroupService(RestTemplate restTemplate, Properties properties, AuthenticationSetup authenticationSetup, DynamicSSE dynamicSSE){
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.authenticationSetup = authenticationSetup;
        this.dynamicSSE = dynamicSSE;
        ClassPathResource cpr = new ClassPathResource("a_160_512.properties");
        PairingParameters pairingParameters = PairingFactory.getPairingParameters(cpr.getPath());
        PairingKeySerPair keyPair = engine.setup(pairingParameters, 50);//50=max attributes nº
    }

    public ResponseEntity<?> createGroup(CreateGroup group){
        try {
            group.getParticipants().add(properties.getUser());
            ResponseEntity<?> response = restTemplate.exchange(properties.getServerAddress() + "/create/topic",
                    HttpMethod.POST, authenticationSetup.getHeader(group), String.class);
            log.info("Group Created Successfully");
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<?> accessGroup(String name){
        try {
            ResponseEntity<ABEKeySharing> response = restTemplate.exchange(properties.getServerAddress() + "/subscribe/"+name,
                    HttpMethod.GET, authenticationSetup.getHeader(), ABEKeySharing.class);
            ABEKeySharing groupData = response.getBody();
            PairingKeySerParameter secretKey = deSerialize(groupData.getSerializePairingKeySerParameter());
            PairingKeySerParameter publicKey = deSerialize(groupData.getSerializedPublicKey());
            PairingCipherSerParameter header = deSerialize(groupData.getSerializedPairingCipherSerParameter());
            List<String> addresses = groupData.getParticipantAddresses();
            byte[] anSessionKey = engine.decapsulation(publicKey, secretKey,
                    new String[] {String.valueOf(groupData.getId()),
                            String.valueOf(Math.abs(properties.getUser().hashCode()) % 50)},
                    header);
            log.info("Group Key: "+ Base64.getEncoder().encodeToString(anSessionKey));
            Group group = Group.builder()
                    .name(name)
                    .participantsAddresses(addresses)
                    .key(new SecretKeySpec(Arrays.copyOfRange(anSessionKey, 0, 16), "AES/GCM/NoPadding"))
                    .messages(new ArrayList<>())
                    .build();
            groups.put(name, group);
            requestCloudMessages(name);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
        }
    }

    private boolean requestCloudMessages(String group){
        try {
            ResponseEntity<List<MessageDTO>> response = restTemplate.exchange(properties.getCloudAddress() + "/messages/group/"+group
                    , HttpMethod.GET, authenticationSetup.getHeader(), new ParameterizedTypeReference<List<MessageDTO>>() {});
            if(response.getBody() == null)
                return false;
            if(response.getBody().size() == 0)
                return false;
            groups.get(group).getMessages().addAll(response.getBody());
            log.info("Messages received from cloud:"+response.getBody().toString());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
    public <T> T deSerialize(String serializedKey){
        final byte[] bytes = Base64.getDecoder().decode(serializedKey.getBytes());
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private <T> String serialize(T key) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(key);
            final byte[] byteArray = bos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity sendMessage(String message, String groupName) {
        Group group = groups.get(groupName);
        if(group == null)
            return ResponseEntity.notFound().build();
        try {
            log.info("Sending encrypted message to friend");
            IvParameterSpec iv = generateIv();
            String encryptedMessage = encrypt(message, groupName,iv);
            MessageDTO messageDTO = MessageDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .receiver(groupName)
                    .sender(properties.getUser())
                    .content(encryptedMessage)
                    .timestamp(Instant.now())
                    .iv(iv.getIV())
                    .build();
            for(String address : group.getParticipantsAddresses()){
                if(address.equals(properties.getAddress()))
                    continue;
                sendMessage(address, messageDTO);
                log.info("Message sent to: "+address);
            }
            if(sendMessage(properties.getCloudAddress(),messageDTO)){
                log.info("Message sent to cloud");
                String[] splited = message.split("\\s+");
                for(String s:splited){
                    dynamicSSE.update(s,messageDTO.getId(),groups.get(groupName).getKey());
                }
            }
            group.getMessages().add(messageDTO);
            return ResponseEntity.ok().body(messageDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> receiveGroupMsg(MessageDTO messageDTO){
        Group group = groups.get(messageDTO.getReceiver());
        if(group == null)
            return ResponseEntity.notFound().build();
        log.info("Received group message: "+messageDTO);
        group.getMessages().add(messageDTO);
        try{
            MessageUI messageUI = decrypt(messageDTO, group.getKey());
            String[] splited = messageUI.getContent().split("\\s+");
            for(String s:splited){
                dynamicSSE.update(s,messageDTO.getId(),group.getKey());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.ok("Message received from group: "+messageDTO.getReceiver());
    }


    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
    public MessageUI decrypt(MessageDTO message, SecretKey secretKey) {
        try {

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(message.getIv()));
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(message.getContent()));

            return MessageUI.builder()
                    .id(message.getId())
                    .content(new String(plainText))
                    .sender(message.getSender())
                    .receiver(message.getReceiver())
                    .timestamp(message.getTimestamp())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    public String encrypt(String message,String group, IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, groups.get(group).getKey(), iv);
        byte[] cipherText = cipher.doFinal(message.getBytes());

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public boolean sendMessage(String receiverAddress,MessageDTO message) {
        try {
            ResponseEntity<?> response = restTemplate.exchange(receiverAddress+ "/receive/group/message",
                    HttpMethod.POST, authenticationSetup.getHeader(message), String.class);
            log.info("Message sent to"+receiverAddress);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public ResponseEntity<?> getGroupMessage(String group) {
        Group group1 = groups.get(group);
        if(group1 == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(group1.getMessages().stream().map(messageDTO -> decrypt(messageDTO, group1.getKey()))
                .collect(Collectors.toList()));
    }

    public List<MessageUI> searchMessagesFromGroup(String group,String word) {
        Group group1 = groups.get(group);
        if(group1 == null) {
            log.info("Group not found");
            return new ArrayList<>();
        }
        try {
            List<MessageDTO> messages = messages = dynamicSSE.search(word,group1.getKey());
            log.info("Messages found: "+messages);
            return messages.stream().map(message -> {
                log.info("Encrypted message: "+ message);
                MessageUI messageUI = decrypt(message,group1.getKey());
                log.info("Decrypted message: "+messageUI);
                return messageUI;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


}
