package org.psd.CloudPSD.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.CloudPSD.models.network.ShareDTO;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "t_share_cloud1")
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String user1;
    private String user2;
    private  BigInteger shareholder;
    private  String share;

    public Share(String user1, String user2, BigInteger shareholder, BigInteger share) {
        this.user1 = user1;
        this.user2 = user2;
        this.shareholder = shareholder;
        this.share = share.toString();
    }
    public ShareDTO toShareDto(){
        return new ShareDTO(user1,user2,shareholder, new BigInteger(share));
    }
}
