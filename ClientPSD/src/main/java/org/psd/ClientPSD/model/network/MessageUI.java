package org.psd.ClientPSD.model.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
public class MessageUI {
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private Instant timestamp;
}
