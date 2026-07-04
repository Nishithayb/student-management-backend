package com.sms.studentmanagementbackend.security;

import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return appUserRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or email"));
    }

    public CustomUserDetails loadById(Long userId) {
        return appUserRepository.findById(userId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
