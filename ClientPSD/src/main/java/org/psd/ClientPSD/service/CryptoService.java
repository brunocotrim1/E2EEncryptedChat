package org.psd.ClientPSD.service;

import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.configuration.AuthenticationSetup;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.crypto.SecretSharing;
import org.psd.ClientPSD.model.Friend;
import org.psd.ClientPSD.model.IBEFriendEncapsulation;
import org.psd.ClientPSD.model.Share;
import org.psd.ClientPSD.model.network.MessageDTO;
import org.psd.ClientPSD.model.network.MessageUI;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CryptoService {

    private HashMap<String,Friend> friends= new HashMap<>();
    RestTemplate restTemplate;
    Properties properties;

    AuthenticationSetup authenticationSetup;

    IBECypherService ibeCypherService;

    public CryptoService(RestTemplate restTemplate, Properties properties, AuthenticationSetup authenticationSetup,IBECypherService ibeCypherService) {
        this.ibeCypherService = ibeCypherService;
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.authenticationSetup = authenticationSetup;
    }


    public boolean addFriend(String user) {
        String address = getUserAddress(user);
        if(address == null){
            return false;
        }
        if(friends.get(user)!=null){
            log.info("Friend already added");
            return false;
        }
        Friend friend = Friend.builder().address(address).username(user).messages(new ArrayList<>()).build();
        friends.put(user, friend);
        if(requestSecretKeyHeaders(user,address)){
            System.out.println("Secret key headers received "+Base64.getEncoder().encodeToString(friend.getSecretKey().getEncoded()));
            Share[] shares = SecretSharing.generateKeyShare(friend.getSecretKey(),properties.getUser(),user);
            friend.setShare(shares[2]);
            sendShareToFriend(user,shares[0]);
            sendShareToServer(user,shares[1]);
            sendShareToCloud(user,shares[3]);
            restoreMessages(friend.getUsername());
            return true;
        }
        Share[] sharesToCombine = getSharesToReconstruct(user);

        if(sharesToCombine.length < SecretSharing.threshold) {
            IBEFriendEncapsulation ibeFriendEncapsulation = ibeCypherService.encapsulateKey(user);
            friend.setHeaderSecretKey(ibeFriendEncapsulation.getSecretKeyHeader());
            friend.setSecretKey(ibeFriendEncapsulation.getSecretKey());
            System.out.println("Secret key: " + Base64.getEncoder().encodeToString(friend.getSecretKey().getEncoded()));
            restoreMessages(friend.getUsername());
        }else{
            SecretKey secretKey = SecretSharing.combineKeyShares(sharesToCombine);
            log.info("Secret key reconstructed: "+Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            friend.setSecretKey(secretKey);
            restoreMessages(friend.getUsername());
        }
        return true;
    }




    private Share[] getSharesToReconstruct(String user){
        List<Share> sharesToCombine = new ArrayList();
        Share myShare = getShare(properties.getUser(), user);
        if(myShare != null)
            sharesToCombine.add(myShare);

        Share friendShare = requestFriendShare(user);
        if(friendShare != null)
            sharesToCombine.add(friendShare);

        Share serverShare = requestServerShare(user);
        if(serverShare != null)
            sharesToCombine.add(serverShare);

        Share cloudShare = requestCloudShare(user);
        if(cloudShare != null)
            sharesToCombine.add(cloudShare);
        Share[] shareArray = new Share[sharesToCombine.size()];
        return sharesToCombine.toArray(shareArray);
    }


    public boolean sendShareToFriend(String user1,Share share) {
        try {
            ResponseEntity<?> response = restTemplate.exchange(friends.get(user1).getAddress() + "/share", HttpMethod.POST, authenticationSetup.getHeader(share), String.class);
            log.info("Share Sent to friend");
            return true;
        } catch (Exception exception) {
            log.info(friends.get(user1).getAddress() + "/share");
            return false;
        }
    }
    public boolean sendShareToServer(String user1,Share share) {
        try {
            ResponseEntity<?> response = restTemplate.exchange(properties.getServerAddress() + "/share", HttpMethod.POST, authenticationSetup.getHeader(share), String.class);
            log.info("Share sent to server");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
    public boolean receiveShare(Share share,String token) {
        log.info("Received share {}", share);
        String user = authenticateUser(token);
        if(user == null)
            return false;
        Friend friend = friends.get(user);
        if(friend == null){
            return false;
        }
        friend.setShare(share);
        return true;
    }


    public String authenticateUser(String token){
        try {
            ResponseEntity<String> response = restTemplate.exchange(properties.getServerAddress() + "/api/auth/verify/"+getToken(token), HttpMethod.GET, authenticationSetup.getHeader(), String.class);
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }
    private String getToken(String token){
        return token.substring(7, token.length());
    }

    private Share requestFriendShare(String user2){
        try {
            ResponseEntity<Share> response = restTemplate.exchange(friends.get(user2).getAddress() + "/share", HttpMethod.GET, authenticationSetup.getHeader(), Share.class);
            log.info("Share received from friend");
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    private Share requestServerShare(String user2){
        try {
            ResponseEntity<Share> response = restTemplate.exchange(properties.getServerAddress() + "/share/"+user2, HttpMethod.GET, authenticationSetup.getHeader(), Share.class);
            log.info("Share received from server");
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean requestCloudMessages(String user2){
        try {
            ResponseEntity<List<MessageDTO>> response = restTemplate.exchange(properties.getCloudAddress() + "/messages/"+user2, HttpMethod.GET, authenticationSetup.getHeader(), new ParameterizedTypeReference<List<MessageDTO>>() {});
            if(response.getBody() == null)
                return false;
            if(response.getBody().size() == 0)
                return false;
            friends.get(user2).setMessages(response.getBody());
            log.info("Messages received from cloud:"+response.getBody().toString());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
    private boolean requestFriendMessages(String user2){
        try {
            ResponseEntity<List<MessageDTO>> response = restTemplate.exchange(friends.get(user2).getAddress() + "/messages", HttpMethod.GET, authenticationSetup.getHeader(), new ParameterizedTypeReference<List<MessageDTO>>() {});
            if(response.getBody() == null)
                return false;
            if(response.getBody().size() == 0)
                return false;
            friends.get(user2).setMessages(response.getBody());
            log.info("Messages received from friend:"+response.getBody().toString());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void restoreMessages(String user2){
        if(requestFriendMessages(user2))
            return;
        requestCloudMessages(user2);
    }



    private Share requestCloudShare(String user2){
        try {
            ResponseEntity<Share> response = restTemplate.exchange(properties.getCloudAddress() + "/share/"+user2, HttpMethod.GET, authenticationSetup.getHeader(), Share.class);
            log.info("Share received from server");
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }
    public Share getShare(String user1, String user2) {
        String friendName = getFriend(user1,user2);
        Friend friend = friends.get(friendName);
        System.out.println(friend);
        return friend == null?null:friend.getShare();
    }


    public String getFriend(String user,String user2) {
        return user.equals(properties.getUser())?user2:user;
    }
    public boolean sendShareToCloud(String user1,Share share) {
        try {
            ResponseEntity<?> response = restTemplate.exchange(properties.getCloudAddress() + "/share", HttpMethod.POST, authenticationSetup.getHeader(share), String.class);
            log.info("Share sent to Cloud");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public String getSecretKey(String user){
        Friend friend = friends.get(user);
        return friend == null?null:friend.getHeaderSecretKey();
    }

    public IBEFriendEncapsulation encapsulateKey(String user){
        return ibeCypherService.encapsulateKey(user);
    }


    public boolean requestSecretKeyHeaders(String user,String address){
        try {
            ResponseEntity<String> response = restTemplate.exchange(address + "/ibe/sessionKey", HttpMethod.GET, authenticationSetup.getHeader(), String.class);
            SecretKey secretKey = ibeCypherService.decapsulateKey(response.getBody());
            friends.get(user).setSecretKey(secretKey);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public String getUserAddress(String user){
        try {
            ResponseEntity<?> response = restTemplate.exchange(properties.getServerAddress() + "/api/auth//address/"+user, HttpMethod.GET, authenticationSetup.getHeader(), String.class);

            log.info(response.getBody().toString());
            return response.getBody().toString();
        } catch (Exception exception) {
            return null;
        }
    }

    public MessageDTO sendMessage(String message, String receiver) {
        Friend friend = friends.get(receiver);
        if(friend == null)
            return null;
        try {
            log.info("Sending encrypted message to friend");
            IvParameterSpec iv = generateIv();
            String encryptedMessage = encrypt(message, receiver,iv);
            MessageDTO messageDTO = MessageDTO.builder()
                    .receiver(receiver)
                    .sender(properties.getUser())
                    .content(encryptedMessage)
                    .timestamp(Instant.now())
                    .iv(iv.getIV())
                    .build();
            if(sendMessage(friend.getAddress(),messageDTO)){
                log.info("Message sent to friend");
            }
            if(sendMessage(properties.getCloudAddress(),messageDTO)){
                log.info("Message sent to cloud");
            }
            friend.getMessages().add(messageDTO);
            return messageDTO;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean sendMessage(String receiverAddress,MessageDTO message) {
        try {
            ResponseEntity<?> response = restTemplate.exchange(receiverAddress+ "/receive/message",
                    HttpMethod.POST, authenticationSetup.getHeader(message), String.class);
            log.info("Message sent to"+receiverAddress);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean receiveMessage(MessageDTO messageDTO, String user2) {
        Friend friend = friends.get(user2);
        if (friend == null)
            return false;
        friend.getMessages().add(messageDTO);
        return true;
    }
    public String encrypt(String message,String receiver, IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, friends.get(receiver).getSecretKey(), iv);
        byte[] cipherText = cipher.doFinal(message.getBytes());

        return Base64.getEncoder().encodeToString(cipherText);
    }


    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }


    public List<MessageUI> getMessages(String user2) {
        Friend friend = friends.get(user2);
        if(friend == null)
            return null;
        List<MessageDTO> messages = friend.getMessages();

        return messages.stream().map(message -> {
            log.info("Encrypted message: "+ message);
            MessageUI messageUI = decrypt(message,friend.getSecretKey());
            log.info("Decrypted message: "+messageUI);
            return messageUI;
        }).collect(Collectors.toList());
    }

    public List<MessageDTO> getMessagesDTO(String user2) {
        Friend friend = friends.get(user2);
        if(friend == null)
            return null;
        log.info("Sending bulk messages to friend:"+friend.getMessages());
        return friend.getMessages();
    }


    public MessageUI decrypt(MessageDTO message,SecretKey secretKey) {
        try {

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(message.getIv()));
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(message.getContent()));

            return MessageUI.builder()
                    .content(new String(plainText))
                    .sender(message.getSender())
                    .receiver(message.getReceiver())
                    .timestamp(message.getTimestamp())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }



}
