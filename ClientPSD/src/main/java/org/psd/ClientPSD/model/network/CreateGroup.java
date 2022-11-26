package org.psd.ClientPSD.model.network;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateGroup {
    private List<String> participants;
    private String name;
}
