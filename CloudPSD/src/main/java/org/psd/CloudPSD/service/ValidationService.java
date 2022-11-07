package org.psd.CloudPSD.service;

import org.psd.CloudPSD.config.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ValidationService {
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Properties properties;
    private String getToken(String token){
        return token.substring(7, token.length());
    }


    public String verifyToken(String token){
        try {
            ResponseEntity<String> response = restTemplate.exchange(properties.getServerAddress() + "/api/auth/verify/"+getToken(token), HttpMethod.GET, getHeader(), String.class);
            return response.getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    public HttpEntity<String> getHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        return request;
    }
}