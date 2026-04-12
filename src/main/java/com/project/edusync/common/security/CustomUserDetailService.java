package com.project.edusync.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // Local cache avoids Redis/Jackson serialization of JPA graphs while cutting auth DB pressure.
    private final Cache<String, UserDetails> userDetailsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(2000)
            .build();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDetailsCache.get(username, key -> userRepository.findByUsernameWithAuthorities(key)
                .map(User.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetails not found user : " + key)));
    }
}
