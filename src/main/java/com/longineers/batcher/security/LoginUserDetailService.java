package com.longineers.batcher.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserDetailService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;


    /**
     * 
     * @todo
     * 
     * 1. Implement a Database-Backed User Store:
       * The Issue: The biggest remaining security risk is the in-memory user service. It uses a single, 
         hardcoded user, which is not suitable for any real-world application.
       * The Fix: We need to create a User entity in the database to store user credentials securely. 
         This would involve creating a users table, a User entity, a UserRepository, and updating the 
         UserDetailsService to load users from the database.
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("user".equals(username)) {
            return new User("user", passwordEncoder.encode("password"), new ArrayList<>());
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}
