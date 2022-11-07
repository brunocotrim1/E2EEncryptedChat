package org.psd.ClientPSD.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Share {
    private String user1;
    private String user2;
    private BigInteger shareholder;
    private BigInteger share;

}
