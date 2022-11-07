package org.psd.ClientPSD.model;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
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

    @Override
    public boolean equals(Object o) {
        return username.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
