package org.psd.ClientPSD.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.ClientPSD.model.network.MessageDTO;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Objects;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Friend {
    private String username;
    private String address;
    private Share share;
    private SecretKey secretKey;
    private String headerSecretKey;
    private List<MessageDTO> messages;

    @Override
    public boolean equals(Object o) {
        return username.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
