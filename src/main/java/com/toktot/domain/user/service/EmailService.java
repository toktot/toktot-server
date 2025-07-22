package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${toktot.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async("emailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[똑똣] 이메일 인증 코드");

            String htmlContent = buildVerificationEmailHtml(verificationCode);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("인증 이메일 발송 성공 - email: {}", toEmail);

        } catch (MailException | MessagingException e) {
            log.error("인증 이메일 발송 실패 - email: {}, error: {}", toEmail, e.getMessage(), e);
            throw new ToktotException(ErrorCode.EMAIL_SEND_FAILED, "인증 이메일 발송에 실패했습니다.");
        } catch (Exception e) {
            log.error("인증 이메일 발송 중 예상치 못한 오류 - email: {}, error: {}", toEmail, e.getMessage(), e);
            throw new ToktotException(ErrorCode.EMAIL_SEND_FAILED, "인증 이메일 발송에 실패했습니다.");
        }
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[똑똣] 비밀번호 재설정");

            String htmlContent = buildPasswordResetEmailHtml(resetToken);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("비밀번호 재설정 이메일 발송 성공 - email: {}", toEmail);

        } catch (MailException | MessagingException e) {
            log.error("비밀번호 재설정 이메일 발송 실패 - email: {}, error: {}", toEmail, e.getMessage(), e);
            throw new ToktotException(ErrorCode.EMAIL_SEND_FAILED, "비밀번호 재설정 이메일 발송에 실패했습니다.");
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 중 예상치 못한 오류 - email: {}, error: {}", toEmail, e.getMessage(), e);
            throw new ToktotException(ErrorCode.EMAIL_SEND_FAILED, "비밀번호 재설정 이메일 발송에 실패했습니다.");
        }
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String toEmail, String nickname) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[똑똣] 회원가입을 환영합니다! 🎉");

            String htmlContent = buildWelcomeEmailHtml(nickname);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("환영 이메일 발송 성공 - email: {}, nickname: {}", toEmail, nickname);

        } catch (MailException | MessagingException e) {
            log.error("환영 이메일 발송 실패 - email: {}, error: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("환영 이메일 발송 중 예상치 못한 오류 - email: {}, error: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildVerificationEmailHtml(String verificationCode) {
        try {
            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);

            String html = templateEngine.process("email/verification", context);
            log.debug("이메일 인증 HTML 템플릿 생성 성공");
            return html;

        } catch (Exception e) {
            log.error("이메일 인증 HTML 템플릿 생성 실패: {}", e.getMessage(), e);

            return buildFallbackVerificationHtml(verificationCode);
        }
    }

    private String buildPasswordResetEmailHtml(String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("verificationCode", resetToken);

            String html = templateEngine.process("email/password-reset", context);
            log.debug("비밀번호 재설정 HTML 템플릿 생성 성공");
            return html;

        } catch (Exception e) {
            log.error("비밀번호 재설정 HTML 템플릿 생성 실패: {}", e.getMessage(), e);

            return buildFallbackPasswordResetHtml(resetToken);
        }
    }

    private String buildWelcomeEmailHtml(String nickname) {
        try {
            Context context = new Context();
            context.setVariable("nickname", nickname);
            context.setVariable("frontendUrl", frontendUrl);

            String html = templateEngine.process("email/welcome", context);
            log.debug("환영 이메일 HTML 템플릿 생성 성공 - nickname: {}", nickname);
            return html;

        } catch (Exception e) {
            log.error("환영 이메일 HTML 템플릿 생성 실패: {}", e.getMessage(), e);

            return buildFallbackWelcomeHtml(nickname);
        }
    }

    private String buildFallbackVerificationHtml(String verificationCode) {
        log.warn("기본 인증 이메일 템플릿 사용 - verificationCode: {}", verificationCode);
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #ff6b35;">똑똣 이메일 인증</h2>
                        <p>인증 코드: <strong style="font-size: 24px; color: #ff6b35;">%s</strong></p>
                        <p>본인이 요청하지 않은 경우, 이 이메일을 무시해주세요.</p>
                    </div>
                </body>
                </html>
                """, verificationCode);
    }

    private String buildFallbackPasswordResetHtml(String resetToken) {
        log.warn("기본 비밀번호 재설정 이메일 템플릿 사용 - resetToken: {}", resetToken);
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <p>재설정 코드: <strong>%s</strong></p>
                        <p>본인이 요청하지 않았다면 이 이메일을 무시해주세요.</p>
                    </div>
                </body>
                </html>
                """, resetToken);
    }

    private String buildFallbackWelcomeHtml(String nickname) {
        log.warn("기본 환영 이메일 템플릿 사용 - nickname: {}", nickname);
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #ff6b35;">똑똣에 오신 것을 환영합니다! 🎉</h2>
                        <p>안녕하세요, <strong>%s</strong>님!</p>
                        <p>똑똣 회원이 되어주셔서 감사합니다.</p>
                        <p>이제 제주도의 맛있는 음식들을 합리적인 가격으로 찾아보세요!</p>
                    </div>
                </body>
                </html>
                """, nickname);
    }
}
