package org.psd.ClientPSD.model.network;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.psd.ClientPSD.model.Direction;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
public class MessageDTO {
    private String sender;
    private String receiver;
    private String content;
    private byte[] iv;
    private Direction direction;
    private Instant timestamp;
}
