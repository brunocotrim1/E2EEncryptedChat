package org.psd.server.ServerPSD.service;

import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeyEncapsulationSerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import lombok.extern.slf4j.Slf4j;
import org.psd.server.ServerPSD.model.GroupChat;
import org.psd.server.ServerPSD.model.User;
import org.psd.server.ServerPSD.model.network.ABEKeySharing;
import org.psd.server.ServerPSD.model.network.CreateGroup;
import org.psd.server.ServerPSD.repositories.IGroupChatRepository;
import org.psd.server.ServerPSD.repositories.IUsersRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopicService {

    private final IGroupChatRepository iGroupChatRepository;
    private final IUsersRepository iUsersRepository;

    private KPABEEngine engine;
    private PairingParameters pairingParameters;

    private PairingKeySerPair keyPair;


    public TopicService(IGroupChatRepository topicRepository, IUsersRepository iUsersRepository) {
        this.iGroupChatRepository = topicRepository;
        this.iUsersRepository = iUsersRepository;
        ClassPathResource cpr = new ClassPathResource("a_160_512.properties");
        engine = KPABEGPSW06aEngine.getInstance();
        pairingParameters =  PairingFactory.getPairingParameters(cpr.getPath());
        keyPair = engine.setup(pairingParameters, 50);//50=max attributes nº
    }

    @Transactional
    public ResponseEntity<?> createGroup(CreateGroup createGroup){
        Optional<GroupChat> GroupChatOptional = iGroupChatRepository.findByName(createGroup.getName());
        if(GroupChatOptional.isPresent()){
            return ResponseEntity.badRequest().body("Group already exists");
        }
        int id = new SecureRandom().nextInt();
        id = Math.abs(id);
        List<Integer> participants = createGroup.getParticipants().stream().map(name -> Math.abs(name.hashCode()) % 50)
                .collect(Collectors.toList());
        participants.add(id % 50);
        List<String> attributes = participants.stream().map(String::valueOf).collect(Collectors.toList());
        String [] attributesArray = attributes.toArray(new String[attributes.size()]);
        for (String attribute : attributesArray) {
            System.out.println(attribute);
        }
        PairingKeyEncapsulationSerPair encapsulationPair = engine.encapsulation(keyPair.getPublic(), attributesArray);
        GroupChat groupChat = GroupChat.builder().id(id).name(createGroup.getName())
                .participants(createGroup.getParticipants())
                .serializedKeyEncapsulationPair(serialize(encapsulationPair.getHeader()))
                .build();
        iGroupChatRepository.save(groupChat);
        log.info("Chat key: {}", Base64.getEncoder().encodeToString(encapsulationPair.getSessionKey()));
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> accessGroup(String user,String name) throws PolicySyntaxException {
        Optional<GroupChat> GroupChatOptional = iGroupChatRepository.findByName(name);
        if(!GroupChatOptional.isPresent()){
            return ResponseEntity.badRequest().body("Group does not exist");
        }
        GroupChat groupChat = GroupChatOptional.get();
        if(!groupChat.getParticipants().contains(user)){
            return ResponseEntity.badRequest().body("User not in group");
        }
        String policy = String.valueOf(groupChat.getId() % 50)+ " and "+ (Math.abs(user.hashCode()) % 50);
        int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
        String[] rhos = ParserUtils.GenerateRhos(policy);
        PairingKeySerParameter secretKey = engine.keyGen(keyPair.getPublic(), keyPair.getPrivate(), accessPolicy, rhos);
        PairingCipherSerParameter header = deSerialize(groupChat.getSerializedKeyEncapsulationPair());
        log.info("Chat key: {}", serialize(header));
        log.info("Secret key: {}", serialize(secretKey));

        List<String> addresses= new ArrayList<>();
        for (String participant : groupChat.getParticipants()) {
            Optional<User> userOpt = iUsersRepository.findById(participant);
            if(userOpt.isPresent()){
                addresses.add(userOpt.get().getAddress());
            }
        }

        ABEKeySharing keySharing = ABEKeySharing.builder().serializePairingKeySerParameter(serialize(secretKey))
                .serializedPairingCipherSerParameter(serialize(header))
                .serializedPublicKey(serialize(keyPair.getPublic()))
                .participantAddresses(addresses)
                .name(groupChat.getName())
                .id(groupChat.getId() % 50)
                .build();
        return ResponseEntity.ok(keySharing);
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
