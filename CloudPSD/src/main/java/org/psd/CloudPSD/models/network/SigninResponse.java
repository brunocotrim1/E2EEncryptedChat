package org.psd.CloudPSD.models.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SigninResponse {

    private String username;
    private String refreshToken;
    private String accessToken;

}
