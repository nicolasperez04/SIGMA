package com.SIGMA.USCO.config;

import com.SIGMA.USCO.common.exception.InternalException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String message) {

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(to);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }

    public void sendEmailWithAttachment(String to, String subject, String message, File attachment, String attachmentName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message);

            if (attachment != null && attachment.exists()) {
                FileSystemResource file = new FileSystemResource(attachment);
                helper.addAttachment(attachmentName, file);
                log.info("Archivo adjunto agregado: {}", attachmentName);
            }

            mailSender.send(mimeMessage);
            log.info("Correo con adjunto enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            log.error("Error enviando correo con adjunto: {}", e.getMessage(), e);
            throw new InternalException("Error al enviar correo con adjunto", e);
        }
    }
}
