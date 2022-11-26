package org.psd.ClientPSD.model.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ABEKeySharing {
    public String serializedPairingCipherSerParameter;
    public String serializePairingKeySerParameter;
    public String serializedPublicKey;
    public List<String> participantAddresses;
    public String name;
    public int id;
}
