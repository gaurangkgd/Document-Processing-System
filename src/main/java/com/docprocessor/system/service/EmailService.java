package com.docprocessor.system.service;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.from:no-reply@docprocessor.example.com}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to Document Processing System";
        String html = "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\"/>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background:#2a9df4; color:#fff; padding:16px; border-radius:6px 6px 0 0; }"
                + ".content { background:#fff; padding:20px; border:1px solid #e6e6e6; }"
                + ".button { display:inline-block; padding:10px 16px; background:#2a9df4; color:#fff; text-decoration:none; border-radius:4px; }"
                + ".footer { font-size:12px; color:#888; margin-top:16px; }"
                + "</style></head><body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h2>Document Processing System</h2></div>"
                + "<div class=\"content\">"
                + "<h3>Hello " + escapeHtml(username) + ",</h3>"
                + "<p>Welcome to the Document Processing System. You can upload documents, create processing jobs, and review results from your dashboard.</p>"
                + "<p><a class=\"button\" href=\"https://app.example.com/dashboard\">Go to Dashboard</a></p>"
                + "<p class=\"footer\">If you did not sign up for this account, please ignore this email.</p>"
                + "</div></div></body></html>";
        sendEmail(toEmail, subject, html);
    }

    public void sendJobCompletedEmail(String toEmail, String username, Long jobId, String documentName) {
        String subject = "Document Processing Completed";
        String viewUrl = String.format("https://app.example.com/jobs/%d/results", jobId);
        String html = "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\"/>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background:#28a745; color:#fff; padding:16px; border-radius:6px 6px 0 0; }"
                + ".content { background:#fff; padding:20px; border:1px solid #e6e6e6; }"
                + ".button { display:inline-block; padding:10px 16px; background:#28a745; color:#fff; text-decoration:none; border-radius:4px; }"
                + ".meta { font-size:14px; color:#555; margin-top:10px; }"
                + "</style></head><body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h2>Processing Completed</h2></div>"
                + "<div class=\"content\">"
                + "<h3>Hello " + escapeHtml(username) + ",</h3>"
                + "<p>The processing job <strong>#" + jobId + "</strong> for document <strong>" + escapeHtml(documentName) + "</strong> has completed successfully.</p>"
                + "<p class=\"meta\">You can view the results and download outputs from the link below.</p>"
                + "<p><a class=\"button\" href=\"" + viewUrl + "\">View Results</a></p>"
                + "<p class=\"footer\">Thank you for using Document Processing System.</p>"
                + "</div></div></body></html>";
        sendEmail(toEmail, subject, html);
    }

    public void sendJobFailedEmail(String toEmail, String username, Long jobId, String documentName, String errorMessage) {
        String subject = "Document Processing Failed";
        String supportUrl = "https://app.example.com/support";
        String html = "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\"/>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background:#dc3545; color:#fff; padding:16px; border-radius:6px 6px 0 0; }"
                + ".content { background:#fff; padding:20px; border:1px solid #e6e6e6; }"
                + ".button { display:inline-block; padding:10px 16px; background:#6c757d; color:#fff; text-decoration:none; border-radius:4px; }"
                + ".error { background:#f8d7da; color:#842029; padding:10px; border-radius:4px; margin-top:10px; }"
                + "</style></head><body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h2>Processing Failed</h2></div>"
                + "<div class=\"content\">"
                + "<h3>Hello " + escapeHtml(username) + ",</h3>"
                + "<p>We're sorry—your processing job <strong>#" + jobId + "</strong> for document <strong>" + escapeHtml(documentName) + "</strong> has failed.</p>"
                + "<div class=\"error\"><strong>Error:</strong> " + escapeHtml(errorMessage) + "</div>"
                + "<p>Please try to re-submit the job. If the problem persists, contact support.</p>"
                + "<p><a class=\"button\" href=\"" + supportUrl + "\">Contact Support</a></p>"
                + "<p class=\"footer\">We apologize for the inconvenience.</p>"
                + "</div></div></body></html>";
        sendEmail(toEmail, subject, html);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping email to {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {} with subject '{}': {}", to, subject, ex.getMessage(), ex);
        }
    }

    // Minimal HTML escaping for injected values to avoid breaking templates.
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
