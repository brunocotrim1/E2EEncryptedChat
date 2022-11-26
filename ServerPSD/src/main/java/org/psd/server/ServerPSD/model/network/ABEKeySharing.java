package org.psd.server.ServerPSD.model.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ABEKeySharing {
    public String serializedPairingCipherSerParameter;
    public String serializePairingKeySerParameter;
    public String serializedPublicKey;
    public List<String> participantAddresses;
    public String name;
    public int id;
}
