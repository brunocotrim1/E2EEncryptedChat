package org.psd.server.ServerPSD.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.server.ServerPSD.exceptions.InvalidCredentialsException;
import org.psd.server.ServerPSD.model.RefreshToken;
import org.psd.server.ServerPSD.model.User;
import org.psd.server.ServerPSD.model.network.SigninResponse;
import org.psd.server.ServerPSD.model.network.SignupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Data
@NoArgsConstructor
public class AccountService {

    @Autowired
    UserService userService;
    @Autowired
    TokenService tokenService;
    @Autowired
    PasswordEncoder passwordEncoder;

    public void register(String username, String password,String address) {
        User user = userService.addUser(new User(username, password,address));
    }

    @Transactional
    public SigninResponse signIn(String username, String password) {
        try {
            User user = userService.getUser(username);

            if (passwordEncoder.matches(password, user.getPassword())) {
                tokenService.deleteRefreshToken(username);
                RefreshToken token = tokenService.creteRefreshToken(username);
                String accessToken = tokenService.createAccessToken(username);
                return new SigninResponse(user.getUsername(), token.getToken(), accessToken);
            } else {
                throw new InvalidCredentialsException();
            }
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public SignupResponse refreshToken(String token) {
        if (!tokenService.validateRefreshToken(token))
            return null;
        RefreshToken refreshToken = tokenService.getRefreshToken(Sha512DigestUtils.shaHex(token));

        if (refreshToken == null)
            return null;
        String username = tokenService.getUsernameFromRefreshToken(token);
        if (!username.equals(refreshToken.getUser().getUsername())) {
            return null;
        }
        String accesstoken = tokenService.createAccessToken(username);
        return new SignupResponse(username, accesstoken);

    }

    public String verifyToken(String token) {
        return tokenService.verifyToken(token);
    }

    public String getAddress(String id) {
        User user = userService.getUser(id);
        return user == null ? null : user.getAddress();
    }
}
