package org.psd.CloudPSD.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.CloudPSD.models.network.MessageDTO;

import javax.persistence.*;
import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@Entity
@NoArgsConstructor
public class Message {

    @Id
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private byte[] iv;
    private Instant timestamp;
    @Enumerated(EnumType.STRING)
    private MessageType type;
    public Message(MessageDTO messageDTO, MessageType type) {
        this.id = messageDTO.getId();
        this.sender = messageDTO.getSender();
        this.receiver = messageDTO.getReceiver();
        this.content = messageDTO.getContent();
        this.iv = messageDTO.getIv();
        this.timestamp = messageDTO.getTimestamp();
        this.type = type;
    }

    public MessageDTO toDTO(){
        return MessageDTO.builder()
                .id(this.id)
                .sender(this.sender)
                .receiver(this.receiver)
                .content(this.content)
                .iv(this.iv)
                .timestamp(this.timestamp)
                .build();
    }
}
