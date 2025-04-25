package com.blackcode.app_check_server.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;


    @Async
    public void sendEmail(
            String to,
            String subject,
            String name,
            String message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z");
        DateTimeFormatter outputFormatter1 = DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm:ss");
        DateTimeFormatter outputFormatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");


        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom("firdausmzidane@gmail.com");
        helper.setSubject("Payment Revenue Payment Notification");

        // Load template Thymeleaf
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("message", message);
        String htmlContent = templateEngine.process("email-template", context);
        helper.setText(htmlContent, true);

        // Tambahkan gambar inline
//        ClassPathResource logo1 = new ClassPathResource("static/pengayomanImigrasi.png");
//        ClassPathResource logo2 = new ClassPathResource("static/imigrasi.png");
//        ClassPathResource logo3 = new ClassPathResource("static/BRI.png");
//        ClassPathResource logo4 = new ClassPathResource("static/kemenkeuLong.png");
//        ClassPathResource logo5 = new ClassPathResource("static/kemenkeu.png");
//        ClassPathResource successIcon = new ClassPathResource("static/success.png");
//
//        helper.addInline("logo1", logo1);
//        helper.addInline("logo2", logo2);
//        helper.addInline("logo3", logo3);
//        helper.addInline("logo4", logo4);
//        helper.addInline("logo5", logo5);
        mailSender.send(mimeMessage);
    }
}
