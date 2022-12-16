package org.psd.ClientPSD.configuration;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.model.network.IBEKeySharing;
import org.psd.ClientPSD.model.network.SigninResponse;
import org.psd.ClientPSD.model.network.SignupRequest;
import org.psd.ClientPSD.model.network.SignupResponse;
import org.psd.ClientPSD.service.IBECypherService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Base64;

@Configuration
@Slf4j
public class AuthenticationSetup {
    private RestTemplate restTemplate;
    HttpsRestConfig httpsRestConfiguration;
    public String refreshToken;
    public String accessToken;
    Properties properties;

    IBECypherService ibeCypherService;
    public AuthenticationSetup(Properties properties, HttpsRestConfig httpsRestConfiguration,IBECypherService ibeCypherService) {
        this.restTemplate = new RestTemplate();
        httpsRestConfiguration.customize(restTemplate);
        this.properties = properties;
        this.ibeCypherService = ibeCypherService;
        register();
        login();
        test(accessToken);
        getIBEKeys();
    }


    public void register() {
        SignupRequest request = new SignupRequest(properties.getUser(), properties.getPassword(),properties.getAddress());
        try{
            ResponseEntity<?> response = restTemplate.postForEntity(properties.getServerAddress() + "/api/auth/register", request, String.class);
        }
        catch (Exception e){
        }
    }

    public void getIBEKeys(){
        try {
            ResponseEntity<IBEKeySharing> response = restTemplate.exchange(properties.getServerAddress() + "/ibe/generate/secretKey", HttpMethod.GET, getHeader(), IBEKeySharing.class);
            ibeCypherService.setPublicKey(deSerialize(response.getBody().getPublicKey()));
            ibeCypherService.setSecretKey(deSerialize(response.getBody().getSecretKey()));
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public PairingKeySerParameter deSerialize(String serializedKey){
        final byte[] bytes = Base64.getDecoder().decode(serializedKey.getBytes());
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (PairingKeySerParameter) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void login() {
        SignupRequest request = new SignupRequest(properties.getUser(), properties.getPassword(),null);
        ResponseEntity<SigninResponse> response = restTemplate.postForEntity(properties.getServerAddress() + "/api/auth/login", request, SigninResponse.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            refreshToken = response.getBody().getRefreshToken();
            accessToken = response.getBody().getAccessToken();
        }
    }
    public void test(String accessTokenParam){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+accessTokenParam);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        ResponseEntity<String> response = restTemplate.exchange(properties.getServerAddress() + "/message", HttpMethod.GET,request, String.class);
        log.warn( "\u001b["  // Prefix - see [1]
                + "20"        // Brightness
                + ";"        // Separator
                + "32"       // Red foreground
                + "m"        // Suffix
                + "AUTHENTICATION SUCCESSFUL"       // the text to output
                + "\u001b[m "); // Prefix + Suffix to reset color);
    }

    @Scheduled(fixedDelay = 1000 * 60 * 1)
    public void refreshToken() {
        if(refreshToken == null)
            return;
        try {
            ResponseEntity<SignupResponse> response = restTemplate.exchange(properties.getServerAddress() + "/api/auth/refresh/" + refreshToken, HttpMethod.GET, null, SignupResponse.class);
            accessToken = response.getBody().getToken();
        }
        catch (Exception e){
            e.printStackTrace();
            register();
            login();
        }finally {
            test(accessToken);
        }
    }

    public HttpEntity<String> getHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+accessToken);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        return request;
    }
    public <T> HttpEntity<T> getHeader(T body){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+accessToken);
        HttpEntity<T> request = new HttpEntity<T>(body,headers);
        return request;
    }
}
