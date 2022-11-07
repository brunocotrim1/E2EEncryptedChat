package org.psd.server.ServerPSD.service;

import lombok.extern.slf4j.Slf4j;
import org.psd.server.ServerPSD.model.Share;
import org.psd.server.ServerPSD.model.network.ShareDTO;
import org.psd.server.ServerPSD.repositories.IShareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShareService {
    @Autowired
    private IShareRepository shareRepository;

    public Share storeShare(ShareDTO shareDTO){
        Share getShareByUsers = shareRepository.findByUser1AndUser2(shareDTO.getUser1(), shareDTO.getUser2());
        if(getShareByUsers != null){
            shareRepository.delete(getShareByUsers);
        }
        return shareRepository.save(shareDTO.toShare())==null?null:shareDTO.toShare();
    }

    public Share getShareByUsers(String user1,String user2){
        return shareRepository.findByUser1AndUser2(user1,user2);
    }

    public void deleteShareByUsers(String user1,String user2){
        Share share = getShareByUsers(user1,user2);
        if(share != null)
            shareRepository.delete(share);
    }




}
