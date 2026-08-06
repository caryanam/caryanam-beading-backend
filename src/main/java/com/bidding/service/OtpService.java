package com.bidding.service;

import com.bidding.repo.AdminRepository;
import com.bidding.repo.DealerRepository;
import com.bidding.repo.InspectorRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final DealerRepository dealerRepository;
    private final InspectorRepository inspectorRepository;
    private final AdminRepository adminRepository;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class OtpData {
        private String otp;
        private LocalDateTime expiryTime;
    }

    public void sendOtp(String email, String mobile) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();

        // Check if email already registered
        if (dealerRepository.existsByEmail(cleanEmail) ||
            inspectorRepository.existsByEmail(cleanEmail) ||
            adminRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException("Email address is already registered. Please sign in.");
        }

        // Check if mobile number already registered
        if (mobile != null && !mobile.trim().isEmpty()) {
            String cleanMobile = mobile.trim();
            if (dealerRepository.existsByMobileNumber(cleanMobile) ||
                inspectorRepository.existsByMobileNumber(cleanMobile) ||
                adminRepository.existsByMobileNumber(cleanMobile)) {
                throw new IllegalStateException("Mobile number is already registered. Please sign in or use a different mobile number.");
            }
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        otpStorage.put(cleanEmail, new OtpData(otp, expiry));
        verifiedEmails.remove(cleanEmail);

        // Send Rich HTML Email
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("datturathod2333@gmail.com", "Caryanam Bidding");
            helper.setTo(cleanEmail);
            helper.setSubject(otp + " is your Caryanam Bidding Verification Code");

            String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f5f7; margin: 0; padding: 20px; }
              .container { max-width: 520px; margin: 0 auto; background: #0D0E12; border-radius: 24px; overflow: hidden; box-shadow: 0 15px 35px rgba(0,0,0,0.3); color: #ffffff; }
              .header { background: linear-gradient(135deg, #0D0E12 0%, #1A1C23 100%); padding: 36px 24px 28px; text-align: center; border-bottom: 1px solid rgba(255, 199, 0, 0.2); }
              .logo-title { font-size: 20px; font-weight: 900; color: #FFC700; letter-spacing: 2.5px; text-transform: uppercase; margin: 0; }
              .subtitle { font-size: 11px; color: #9E9EA7; margin-top: 6px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 700; }
              .content { padding: 36px 28px; text-align: center; }
              .greeting { font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 12px; }
              .desc { font-size: 13px; color: #B5B5BE; line-height: 1.6; margin-bottom: 28px; max-width: 400px; margin-left: auto; margin-right: auto; }
              .otp-box { background: #16181F; border: 2px dashed #FFC700; border-radius: 16px; padding: 20px; margin: 0 auto 28px; max-width: 320px; }
              .otp-code { font-size: 38px; font-weight: 900; color: #FFC700; letter-spacing: 12px; margin: 0; font-family: 'Courier New', monospace; }
              .badge { display: inline-block; background: rgba(255, 199, 0, 0.1); border: 1px solid rgba(255, 199, 0, 0.3); color: #FFC700; font-size: 11px; font-weight: 700; padding: 6px 18px; border-radius: 20px; margin-bottom: 24px; }
              .footer { background: #08090C; padding: 20px; text-align: center; font-size: 11px; color: #696974; border-top: 1px solid rgba(255,255,255,0.05); }
            </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 class="logo-title">CARYANAM BIDDING</h1>
                  <div class="subtitle">Digital Vehicle Bidding Platform</div>
                </div>
                <div class="content">
                  <div class="greeting">Email Registration Verification</div>
                  <p class="desc">Welcome to Caryanam Bidding! Use the following One-Time Password (OTP) to complete your account registration:</p>
                  
                  <div class="otp-box">
                    <div class="otp-code">{{OTP}}</div>
                  </div>
                  
                  <div class="badge">⏱ Valid for 10 minutes</div>
                  <p style="font-size: 11px; color: #808191; margin: 0;">If you did not request this OTP, please ignore this email.</p>
                </div>
                <div class="footer">
                  &copy; Caryanam Bidding. All rights reserved.<br/>
                  Secure Automobile Liquidation System
                </div>
              </div>
            </body>
            </html>
            """.replace("{{OTP}}", otp);

            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();

        // Check if email exists in system
        if (!dealerRepository.existsByEmail(cleanEmail) &&
            !inspectorRepository.existsByEmail(cleanEmail) &&
            !adminRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException("Email address is not registered in our system.");
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        otpStorage.put(cleanEmail, new OtpData(otp, expiry));
        verifiedEmails.remove(cleanEmail);

        // Send Rich HTML Email for Password Change Verification
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("datturathod2333@gmail.com", "Caryanam Bidding");
            helper.setTo(cleanEmail);
            helper.setSubject(otp + " is your Password Change Verification Code");

            String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f5f7; margin: 0; padding: 20px; }
              .container { max-width: 520px; margin: 0 auto; background: #0D0E12; border-radius: 24px; overflow: hidden; box-shadow: 0 15px 35px rgba(0,0,0,0.3); color: #ffffff; }
              .header { background: linear-gradient(135deg, #0D0E12 0%, #1A1C23 100%); padding: 36px 24px 28px; text-align: center; border-bottom: 1px solid rgba(255, 199, 0, 0.2); }
              .logo-title { font-size: 20px; font-weight: 900; color: #FFC700; letter-spacing: 2.5px; text-transform: uppercase; margin: 0; }
              .subtitle { font-size: 11px; color: #9E9EA7; margin-top: 6px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 700; }
              .content { padding: 36px 28px; text-align: center; }
              .greeting { font-size: 18px; font-weight: 800; color: #ffffff; margin-bottom: 12px; }
              .desc { font-size: 13px; color: #B5B5BE; line-height: 1.6; margin-bottom: 28px; max-width: 400px; margin-left: auto; margin-right: auto; }
              .otp-box { background: #16181F; border: 2px dashed #FFC700; border-radius: 16px; padding: 20px; margin: 0 auto 28px; max-width: 320px; }
              .otp-code { font-size: 38px; font-weight: 900; color: #FFC700; letter-spacing: 12px; margin: 0; font-family: 'Courier New', monospace; }
              .badge { display: inline-block; background: rgba(255, 199, 0, 0.1); border: 1px solid rgba(255, 199, 0, 0.3); color: #FFC700; font-size: 11px; font-weight: 700; padding: 6px 18px; border-radius: 20px; margin-bottom: 24px; }
              .footer { background: #08090C; padding: 20px; text-align: center; font-size: 11px; color: #696974; border-top: 1px solid rgba(255,255,255,0.05); }
            </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 class="logo-title">CARYANAM BIDDING</h1>
                  <div class="subtitle">Digital Vehicle Bidding Platform</div>
                </div>
                <div class="content">
                  <div class="greeting">Password Change Verification</div>
                  <p class="desc">A request was made to change the password for your Caryanam Bidding account. Use the following One-Time Password (OTP) to verify your request:</p>
                  
                  <div class="otp-box">
                    <div class="otp-code">{{OTP}}</div>
                  </div>
                  
                  <div class="badge">⏱ Valid for 10 minutes</div>
                  <p style="font-size: 11px; color: #808191; margin: 0;">If you did not request to change your password, please secure your account immediately.</p>
                </div>
                <div class="footer">
                  &copy; Caryanam Bidding. All rights reserved.<br/>
                  Secure Automobile Liquidation System
                </div>
              </div>
            </body>
            </html>
            """.replace("{{OTP}}", otp);

            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        OtpData stored = otpStorage.get(cleanEmail);

        if (stored == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(stored.getExpiryTime())) {
            otpStorage.remove(cleanEmail);
            return false;
        }

        if (stored.getOtp().equals(otp.trim())) {
            otpStorage.remove(cleanEmail);
            verifiedEmails.put(cleanEmail, true);
            return true;
        }

        return false;
    }

    public boolean isEmailVerified(String email) {
        if (email == null) return false;
        return Boolean.TRUE.equals(verifiedEmails.get(email.trim().toLowerCase()));
    }
}
