package org.psd.ClientPSD.service;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeyEncapsulationSerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.ibe.IBEEngine;
import cn.edu.buaa.crypto.encryption.ibe.bf01a.IBEBF01aEngine;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import lombok.Data;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.IBEFriendEncapsulation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Arrays;
import java.util.Base64;

@Service
@Data
public class IBECypherService
{
    PairingKeySerParameter secretKey;
    PairingKeySerParameter publicKey;
    IBEEngine engine;
    PairingParameters pairingParameters;

    Properties properties;
    public IBECypherService(Properties properties) throws IOException {
        this.properties = properties;
        engine = IBEBF01aEngine.getInstance();
        ClassPathResource cpr = new ClassPathResource("a_160_512.properties");
        pairingParameters = PairingFactory.getPairingParameters(cpr.getPath());
    }

    public IBEFriendEncapsulation encapsulateKey(String user){
        PairingKeyEncapsulationSerPair encapsulationPair = engine.encapsulation(publicKey, user);
        byte[] sessionKey = encapsulationPair.getSessionKey();
        SecretKey k1 = new SecretKeySpec(Arrays.copyOfRange(sessionKey, 0, 16), "AES/GCM/NoPadding");
        String secretKeyHeaderSer = serialize(encapsulationPair.getHeader());
        return new IBEFriendEncapsulation(k1, secretKeyHeaderSer);
    }

    public SecretKey decapsulateKey(String header) throws InvalidCipherTextException {
        PairingCipherSerParameter secretKeyHeader = deSerialize(header);
        byte[] anSessionKey = engine.decapsulation(publicKey, secretKey,properties.getUser(), secretKeyHeader);
        return new SecretKeySpec(Arrays.copyOfRange(anSessionKey, 0, 16), "AES/GCM/NoPadding");
    }

    private String serialize(PairingCipherSerParameter key) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(key);
            final byte[] byteArray = bos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PairingCipherSerParameter deSerialize(String serializedKey){
        final byte[] bytes = Base64.getDecoder().decode(serializedKey.getBytes());
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (PairingCipherSerParameter) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
