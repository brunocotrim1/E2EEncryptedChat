package org.psd.server.ServerPSD.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.psd.server.ServerPSD.exceptions.UserAlreadyExistsException;
import org.psd.server.ServerPSD.exceptions.UserNotFoundException;
import org.psd.server.ServerPSD.model.User;
import org.psd.server.ServerPSD.repositories.IUsersRepository;
import org.psd.server.ServerPSD.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@NoArgsConstructor
@Data
public class UserService implements UserDetailsService {
    @Autowired
    IUsersRepository usersRepository;
    @Autowired
    PasswordEncoder encoder;

    public UserService(IUsersRepository usersRepository,PasswordEncoder encoder) {
        this.usersRepository = usersRepository;
        this.encoder = encoder;
        //this.usersRepository.save(new User("admin", encoder.encode("admin")));
    }

    public User getUser(String username) {
        return usersRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));
    }
    @Transactional
    public User addUser(User user) {
        if(usersRepository.existsById(user.getUsername())) {
            throw new UserAlreadyExistsException(user.getUsername());
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return usersRepository.save(user);
    }



    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = usersRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return UserDetailsImpl.build(user);
    }
}

