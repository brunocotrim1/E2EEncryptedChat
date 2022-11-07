package org.psd.server.ServerPSD.model.network;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IBEKeySharing {
    String publicKey;
    String secretKey;
}
