package org.psd.ClientPSD.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.ClientPSD.model.network.MessageDTO;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Group {
    private String name;
    private List<String> participantsAddresses;
    private SecretKey key;
    private List<MessageDTO> messages;
}
