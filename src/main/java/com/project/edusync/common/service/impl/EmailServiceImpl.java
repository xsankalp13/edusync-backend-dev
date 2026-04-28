package com.project.edusync.common.service.impl;

import com.project.edusync.common.service.EmailService;
import com.project.edusync.iam.model.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from}")
    private String mailFrom;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async // Run this method in a separate thread
    @Override
    public void sendPasswordResetEmail(User user, String token) {
        log.info("Attempting to send password reset email to: {}", user.getEmail());

        // 1. Construct the full reset URL for the frontend
        // Example: http://localhost:3000/reset-password?token=...
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // 2. Prepare the Thymeleaf context
        Context context = new Context();
        context.setVariable("username", user.getUsername()); //
        context.setVariable("resetLink", resetLink);

        try {
            // 3. Process the HTML template
            String htmlContent = templateEngine.process("email/password-reset-email", context);

            // 4. Create and send the MIME message
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail()); //
            helper.setSubject("Shiksha Intelligence - Password Reset");
            helper.setText(htmlContent, true); // true = HTML

            javaMailSender.send(mimeMessage);
            log.info("Password reset email sent successfully to: {}", user.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            // In a production app, you might add this to a retry queue
        }
    }
}