package org.psd.ClientPSD.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.crypto.SecretKey;

@Data
@AllArgsConstructor
public class IBEFriendEncapsulation {
    private SecretKey SecretKey;
    private String secretKeyHeader;
}
