package org.psd.CloudPSD.models.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class SseDTO {
    private String value1;
    private String value2;
    private byte[] iv;
}
