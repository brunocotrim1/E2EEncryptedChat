package org.psd.ClientPSD.service;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import cn.edu.buaa.crypto.encryption.ibe.IBEEngine;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.configuration.AuthenticationSetup;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.Group;
import org.psd.ClientPSD.model.network.ABEKeySharing;
import org.psd.ClientPSD.model.network.CreateGroup;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class GroupService {

    private final RestTemplate restTemplate;
    private final Properties properties;
    private final AuthenticationSetup authenticationSetup;
    private HashMap<String, Group> groups= new HashMap<>();
    private KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
    public GroupService(RestTemplate restTemplate, Properties properties, AuthenticationSetup authenticationSetup){
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.authenticationSetup = authenticationSetup;
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
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.internalServerError().build();
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

}
