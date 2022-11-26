package org.psd.server.ServerPSD.security;

import lombok.NoArgsConstructor;
import org.psd.server.ServerPSD.repositories.IUsersRepository;
import org.psd.server.ServerPSD.security.jwt.AuthEntryPointJWT;
import org.psd.server.ServerPSD.security.jwt.AuthTokenFilter;
import org.psd.server.ServerPSD.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@NoArgsConstructor
public class ApplicationSecurity {
    @Autowired
    private AuthEntryPointJWT unauthorizedHandler;
    @Autowired private IUsersRepository userRepo;
    @Autowired
    UserService userDetailsService;
    @Autowired
    PasswordEncoder passwordEncoder;


    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().antMatchers("/api/auth/**","/h2-console/**","/h2-console").permitAll()
                .anyRequest().authenticated();
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
