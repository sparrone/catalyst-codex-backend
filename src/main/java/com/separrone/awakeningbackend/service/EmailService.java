package com.separrone.awakeningbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String username, String token) throws MessagingException {
        String subject = "Verify your Catalyst Codex account";
        String verificationLink = "http://localhost:8080/api/auth/verify?token=" + token;
        String body = "<p>Hello <b>" + username + "</b>,</p>"
                + "<p>Please verify your email by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify Account</a></p>"
                + "<p>If you didn't create an account, you can ignore this email.</p>"
                + "<p>– Catalyst Codex Team</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    public void sendPasswordChangeEmail(String to) throws MessagingException {
        String subject = "Your Catalyst Codex account password was changed";
        String body = "<p>Hello,</p>"
                + "<p>Your Catalyst Codex account password was successfully changed.</p>"
                + "<p>If you did not perform this action, please reset your password immediately.</p>"
                + "<p>– Catalyst Codex Team</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    public void sendEmailChangeVerification(String username, String newEmail, String token) throws MessagingException {
        String subject = "Confirm your new Catalyst Codex account email";
        String verificationLink = "http://localhost:8080/api/user/verify-email?token=" + token;
        String body = "<p>Hello <b>" + username + "</b>,</p>"
                + "<p>You requested to change your Catalyst Codex account email. Please verify your new email by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify New Email</a></p>"
                + "<p>If you didn’t request this, you can ignore this email.</p>"
                + "<p>– Catalyst Codex Team</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(newEmail);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    public void sendEmailChangeNotification(String username, String oldEmail, String newEmail) throws MessagingException {
        String subject = "Your Catalyst Codex account email change request";
        String body = "<p>Hello <b>" + username + "</b>,</p>"
                + "<p>We received a request to change your Catalyst Codex account email from <b>"
                + oldEmail + "</b> to <b>" + newEmail + "</b>.</p>"
                + "<p>If this wasn’t you, please reset your password immediately to secure your account.</p>"
                + "<p>– Catalyst Codex Team</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(oldEmail);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }
}