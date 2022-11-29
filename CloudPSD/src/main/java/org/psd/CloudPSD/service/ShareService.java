package org.psd.CloudPSD.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.psd.CloudPSD.models.Message;
import org.psd.CloudPSD.models.MessageType;
import org.psd.CloudPSD.models.Share;
import org.psd.CloudPSD.models.network.MessageDTO;
import org.psd.CloudPSD.models.network.ShareDTO;
import org.psd.CloudPSD.repositories.IMessageRepository;
import org.psd.CloudPSD.repositories.IShareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShareService {
    @Autowired
    private IShareRepository shareRepository;

    @Autowired
    private IMessageRepository messageRepository;

    public Share storeShare(ShareDTO shareDTO){
        Share getShareByUsers = shareRepository.findByUser1AndUser2(shareDTO.getUser1(), shareDTO.getUser2());
        if(getShareByUsers != null){
            shareRepository.delete(getShareByUsers);
        }
        return shareRepository.save(shareDTO.toShare())==null?null:shareDTO.toShare();
    }

    public Share getShareByUsers(String user1,String user2){
        return shareRepository.findByUser1AndUser2(user1,user2);
    }

    public void deleteShareByUsers(String user1,String user2){
        Share share = getShareByUsers(user1,user2);
        if(share != null)
            shareRepository.delete(share);
    }


    public boolean storeMessage(MessageDTO messageDTO, String user) {

        Message message = new Message(messageDTO,MessageType.PRIVATE);
        if(!StringUtils.equals(message.getSender(),user))
            return false;
        log.info("Message stored: " + messageDTO);
        return messageRepository.save(message)==null?false:true;
    }

    public boolean storeMessageGroup(MessageDTO messageDTO, String user) {
        Message message = new Message(messageDTO, MessageType.GROUP);
        if(!StringUtils.equals(message.getSender(),user))
            return false;
        log.info("Message stored: " + messageDTO);
        return messageRepository.save(message)==null?false:true;
    }

    public List<MessageDTO> getMessagesGroup(String group){
        return messageRepository.findByGroup(group).stream().map(message -> message.toDTO())
                .collect(Collectors.toList());
    }
    public List<MessageDTO> getMessages(String user1, String user2){
        return messageRepository.findByUser1AndUser2(user1,user2).stream().map(message -> message.toDTO())
                .collect(Collectors.toList());
    }
}
