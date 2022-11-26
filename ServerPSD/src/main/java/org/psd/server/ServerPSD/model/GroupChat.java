package org.psd.server.ServerPSD.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "t_group_chat")
@Builder
public class GroupChat {
    @Id
    private int id;

    @ElementCollection
    private List<String> participants= new ArrayList<String>();

    @Column(unique=true)
    private String name;
    @Lob
    private String serializedKeyEncapsulationPair;
}
