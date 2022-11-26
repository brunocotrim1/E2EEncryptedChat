package org.psd.CloudPSD.models.network;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
public class MessageDTO {
    private String sender;
    private String receiver;
    private String content;
    private byte[] iv;
    private Instant timestamp;
}
