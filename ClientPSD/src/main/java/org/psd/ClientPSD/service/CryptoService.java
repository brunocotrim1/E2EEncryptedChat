package org.psd.ClientPSD.service;

import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.configuration.AuthenticationSetup;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.crypto.SecretSharing;
import org.psd.ClientPSD.model.Friend;
import org.psd.ClientPSD.model.IBEFriendEncapsulation;
import org.psd.ClientPSD.model.Share;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

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


    public void addFriend(String user, String address) {
        if(friends.get(user)!=null){
            log.info("Friend already added");
            return;
        }
        Friend friend = Friend.builder().address(address).username(user).build();
        friends.put(user, friend);
        if(requestSecretKeyHeaders(user,address)){
            System.out.println("Secret key headers received "+Base64.getEncoder().encodeToString(friend.getSecretKey().getEncoded()));
            Share[] shares = SecretSharing.generateKeyShare(friend.getSecretKey(),properties.getUser(),user);
            friend.setShare(shares[2]);
            sendShareToFriend(user,shares[0]);
            sendShareToServer(user,shares[1]);
            sendShareToCloud(user,shares[3]);
            return;
        }
        Share[] sharesToCombine = getSharesToReconstruct(user);

        if(sharesToCombine.length < SecretSharing.threshold) {
            IBEFriendEncapsulation ibeFriendEncapsulation = ibeCypherService.encapsulateKey(user);
            friend.setHeaderSecretKey(ibeFriendEncapsulation.getSecretKeyHeader());
            friend.setSecretKey(ibeFriendEncapsulation.getSecretKey());
            System.out.println("Secret key: " + Base64.getEncoder().encodeToString(friend.getSecretKey().getEncoded()));
        }else{
            SecretKey secretKey = SecretSharing.combineKeyShares(sharesToCombine);
            System.out.println(Base64.getEncoder().encodeToString(secretKey.getEncoded()));
        }
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

    private Share requestCloudShare(String user2){
        try {
            ResponseEntity<Share> response = restTemplate.exchange(properties.getCloudAddress() + "/share/"+user2, HttpMethod.GET, authenticationSetup.getHeader(), Share.class);
            log.info("Share received from server");
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    public void addShare(Share share) {
        String friendName = getFriend(share.getUser1(),share.getUser2());
        Friend friend = friends.get(friendName);
        if(friend != null){
            friend.setShare(share);
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

}
