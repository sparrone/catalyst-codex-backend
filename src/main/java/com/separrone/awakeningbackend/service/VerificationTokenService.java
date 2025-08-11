package com.separrone.awakeningbackend.service;

import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.entity.VerificationToken;
import com.separrone.awakeningbackend.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;

    public VerificationTokenService(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // For initial account verification
    public VerificationToken createTokenForUser(User user) {
        return createToken(user, null);
    }

    // For email change requests
    public VerificationToken createTokenForEmailChange(User user, String newEmail) {
        return createToken(user, newEmail);
    }

    private VerificationToken createToken(User user, String newEmail) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiration(LocalDateTime.now().plusHours(24));
        verificationToken.setNewEmail(newEmail); // can be null for signup
        tokenRepository.save(verificationToken);
        return verificationToken;
    }

    public VerificationToken getToken(String token) {
        return tokenRepository.findByToken(token).orElse(null);
    }

    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }
}