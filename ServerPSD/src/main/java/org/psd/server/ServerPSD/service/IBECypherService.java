package org.psd.server.ServerPSD.service;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.ibe.IBEEngine;
import cn.edu.buaa.crypto.encryption.ibe.bf01a.IBEBF01aEngine;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.psd.server.ServerPSD.model.network.IBEKeySharing;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Base64;


@Service
public class IBECypherService {
    private IBEEngine engine;
    private PairingParameters pairingParameters;

    private PairingKeySerPair keyPair;
    public IBECypherService() {
        ClassPathResource cpr = new ClassPathResource("a_160_512.properties");
        engine = IBEBF01aEngine.getInstance();
        pairingParameters =  PairingFactory.getPairingParameters(cpr.getPath());
        keyPair = engine.setup(pairingParameters);
    }

    public IBEKeySharing getSecretKey(String id) {
        PairingKeySerParameter secretKey = engine.keyGen(keyPair.getPublic(), keyPair.getPrivate(), id);
        String serializedKey = serialize(secretKey);
        String serializedPublic = serialize(keyPair.getPublic());
        System.out.println(deSerialize(serializedPublic));
        return serializedKey == null || serializedPublic == null ? null : new IBEKeySharing(serializedPublic, serializedKey);
    }
    public PairingKeySerParameter deSerialize(String serializedKey){
        final byte[] bytes = Base64.getDecoder().decode(serializedKey.getBytes());
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (PairingKeySerParameter) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private String serialize(PairingKeySerParameter key) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(key);
            final byte[] byteArray = bos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
