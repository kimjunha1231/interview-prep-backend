package com.junha.interview.service;

import com.junha.interview.domain.InterviewHistory;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.node.Node;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private com.junha.interview.common.EncryptionUtils encryptionUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    public void sendReportEmail(String toEmail, Long sessionId, List<InterviewHistory> historyList) {
        log.info("Starting email dispatch process for Session ID: {} to recipient: {}", sessionId, maskEmail(toEmail));

        String htmlContent = buildHtmlReport(sessionId, historyList);

        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Falling back to logging the email content.");
            log.info("================ FALLBACK EMAIL REPORT (SESSION: {}) ================", sessionId);
            log.info("To: {}", maskEmail(toEmail));
            log.info("Subject: [Interview Handbook] 모의 면접 결과 보고서 (Session #{})", sessionId);
            log.info("Content Preview:\n{}", htmlContent);
            log.info("====================================================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Interview Handbook] 모의 면접 결과 보고서 (Session #" + sessionId + ")");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email successfully sent to {}", maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send email via SMTP to {}. Error: {}", maskEmail(toEmail), e.getMessage(), e);
            log.info("================ FALLBACK EMAIL REPORT (SMTP FAILURE) ================");
            log.info("To: {}", maskEmail(toEmail));
            log.info("Subject: [Interview Handbook] 모의 면접 결과 보고서 (Session #{})", sessionId);
            log.info("======================================================================");
        }
    }

    private String buildHtmlReport(Long sessionId, List<InterviewHistory> historyList) {
        double averageScore = historyList.stream()
                .mapToInt(InterviewHistory::getScore)
                .average()
                .orElse(0.0);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Pretendard', 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }");
        sb.append(".container { max-width: 680px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #d2d2d7; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; }");
        sb.append(".header { background: #000000; color: #ffffff; padding: 30px; text-align: center; }");
        sb.append(".header h1 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: -0.5px; }");
        sb.append(".header p { margin: 5px 0 0 0; font-size: 14px; color: #86868b; }");
        sb.append(".summary { padding: 24px; border-bottom: 1px solid #e5e5ea; background: #fafafa; display: flex; justify-content: space-around; text-align: center; }");
        sb.append(".summary-item { flex: 1; }");
        sb.append(".summary-value { font-size: 28px; font-weight: 700; color: #0066cc; }");
        sb.append(".summary-label { font-size: 12px; color: #86868b; margin-top: 4px; text-transform: uppercase; }");
        sb.append(".content { padding: 30px; }");
        sb.append(".question-card { border: 1px solid #e5e5ea; border-radius: 8px; margin-bottom: 24px; overflow: hidden; background: #ffffff; }");
        sb.append(".question-title { background: #fafafa; padding: 16px 20px; border-bottom: 1px solid #e5e5ea; display: flex; justify-content: space-between; align-items: center; }");
        sb.append(".question-title h3 { margin: 0; font-size: 16px; font-weight: 600; color: #1d1d1f; flex: 1; }");
        sb.append(".score-badge { font-size: 12px; font-weight: 600; color: #ffffff; background-color: #0066cc; padding: 4px 8px; border-radius: 20px; white-space: nowrap; margin-left: 10px; }");
        sb.append(".score-badge.low { background-color: #86868b; }");
        sb.append(".card-body { padding: 20px; }");
        sb.append(".section-title { font-size: 11px; font-weight: 700; text-transform: uppercase; color: #86868b; margin: 20px 0 6px 0; letter-spacing: 0.5px; }");
        sb.append(".section-content { font-size: 14px; line-height: 1.6; color: #1d1d1f; margin: 0 0 20px 0; background: #f5f5f7; padding: 12px 16px; border-radius: 6px; }");
        sb.append(".section-content.tail { background: #fff9e6; color: #664d03; border-left: 3px solid #ffcc00; margin-bottom: 0; padding: 12px 16px; border-radius: 6px; }");
        sb.append(".markdown-body { font-size: 14px; line-height: 1.6; color: #1d1d1f; margin: 0 0 20px 0; }");
        sb.append(".markdown-body p { margin: 0 0 12px 0; }");
        sb.append(".markdown-body p:last-child { margin-bottom: 0; }");
        sb.append(".markdown-body ul, .markdown-body ol { margin: 0 0 12px 0; padding-left: 20px; }");
        sb.append(".markdown-body li { margin-bottom: 6px; }");
        sb.append(".markdown-body h1, .markdown-body h2, .markdown-body h3 { font-size: 15px; font-weight: 600; color: #1d1d1f; margin: 18px 0 8px 0; }");
        sb.append(".markdown-body pre { background: #f5f5f7; padding: 12px 16px; border-radius: 6px; overflow-x: auto; font-family: SFMono-Regular, Consolas, monospace; font-size: 13px; margin: 0 0 16px 0; }");
        sb.append(".markdown-body code { font-family: SFMono-Regular, Consolas, monospace; font-size: 13px; background: rgba(0,0,0,0.05); padding: 2px 4px; border-radius: 3px; }");
        sb.append(".markdown-body pre code { background: none; padding: 0; border-radius: 0; }");
        sb.append(".markdown-body.feedback { background: #f0f7ff; color: #003366; border-left: 3px solid #0066cc; padding: 16px; border-radius: 6px; }");
        sb.append(".markdown-body.perfect { background: #f5f5f7; color: #1d1d1f; border-left: 3px solid #86868b; padding: 16px; border-radius: 6px; }");
        sb.append(".footer { background: #f5f5f7; padding: 20px; text-align: center; border-top: 1px solid #e5e5ea; font-size: 12px; color: #86868b; }");
        sb.append(".footer a { color: #0066cc; text-decoration: none; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class='container'>");
        sb.append("  <div class='header'>");
        sb.append("    <h1>Interview Handbook</h1>");
        sb.append("    <p>AI 모의 면접 결과 피드백 보고서</p>");
        sb.append("  </div>");
        sb.append("  <div class='summary'>");
        sb.append("    <div class='summary-item'>");
        sb.append("      <div class='summary-value'>#" + sessionId + "</div>");
        sb.append("      <div class='summary-label'>세션 번호</div>");
        sb.append("    </div>");
        sb.append("    <div class='summary-item'>");
        sb.append("      <div class='summary-value'>" + historyList.size() + "개</div>");
        sb.append("      <div class='summary-label'>면접 질문 수</div>");
        sb.append("    </div>");
        sb.append("    <div class='summary-item'>");
        sb.append("      <div class='summary-value'>" + String.format("%.1f점", averageScore) + "</div>");
        sb.append("      <div class='summary-label'>평균 점수</div>");
        sb.append("    </div>");
        sb.append("  </div>");
        sb.append("  <div class='content'>");

        for (int i = 0; i < historyList.size(); i++) {
            InterviewHistory history = historyList.get(i);
            sb.append("    <div class='question-card'>");
            sb.append("      <div class='question-title'>");
            sb.append("        <h3>Q" + (i + 1) + ". " + history.getQuestion().getTitle() + "</h3>");
            String badgeClass = history.getScore() >= 70 ? "score-badge" : "score-badge low";
            sb.append("        <span class='" + badgeClass + "'>" + history.getScore() + "점</span>");
            sb.append("      </div>");
            sb.append("      <div class='card-body'>");
            
            sb.append("        <h4 class='section-title'>나의 답변</h4>");
            sb.append("        <p class='section-content'>" + escapeHtml(history.getUserAnswer()) + "</p>");

            if (history.getQuestion().getPerfectAnswer() != null && !history.getQuestion().getPerfectAnswer().trim().isEmpty()) {
                sb.append("        <h4 class='section-title'>모범 답안</h4>");
                sb.append("        <div class='markdown-body perfect'>" + markdownToHtml(history.getQuestion().getPerfectAnswer()) + "</div>");
            }

            sb.append("        <h4 class='section-title'>AI 상세 피드백</h4>");
            sb.append("        <div class='markdown-body feedback'>" + markdownToHtml(history.getFeedback()) + "</div>");

            if (history.getTailQuestion() != null && !history.getTailQuestion().trim().isEmpty()) {
                sb.append("        <h4 class='section-title'>추천 꼬리 질문</h4>");
                sb.append("        <p class='section-content tail'>" + escapeHtml(history.getTailQuestion()) + "</p>");
            }

            sb.append("      </div>");
            sb.append("    </div>");
        }

        sb.append("  </div>");
        sb.append("  <div class='footer'>");
        sb.append("    <p>본 메일은 Interview Handbook 서비스에서 발송되었습니다.</p>");
        sb.append("    <p>© 2026 Interview Handbook. All rights reserved.</p>");
        sb.append("  </div>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("\n", "<br/>");
    }

    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    @Async("mailExecutor")
    public void sendDailyQuestionEmail(String toEmail, com.junha.interview.domain.Question question) {
        log.info("Starting Daily Question email dispatch to: {}", maskEmail(toEmail));

        String htmlContent = buildDailyQuestionHtml(toEmail, question);

        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Falling back to logging the daily question email content.");
            log.info("================ FALLBACK DAILY QUESTION EMAIL ================");
            log.info("To: {}", maskEmail(toEmail));
            log.info("Subject: [Daily Handbook] 오늘의 면접 챌린지: {}", question.getTitle());
            log.info("Content Preview:\n{}", htmlContent);
            log.info("===============================================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Daily Handbook] 오늘의 면접 챌린지: " + question.getTitle());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Daily Question email successfully sent to {}", maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send Daily Question email via SMTP to {}. Error: {}", maskEmail(toEmail), e.getMessage(), e);
        }
    }

    private String buildDailyQuestionHtml(String toEmail, com.junha.interview.domain.Question question) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Pretendard', 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #d2d2d7; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; }");
        sb.append(".header { background: #000000; color: #ffffff; padding: 30px; text-align: center; }");
        sb.append(".header h1 { margin: 0; font-size: 22px; font-weight: 600; letter-spacing: -0.5px; }");
        sb.append(".header p { margin: 5px 0 0 0; font-size: 13px; color: #86868b; }");
        sb.append(".content { padding: 30px; }");
        sb.append(".question-title { font-size: 18px; font-weight: 600; color: #0066cc; margin-top: 0; margin-bottom: 24px; line-height: 1.4; }");
        sb.append(".meta-tag { font-size: 10px; font-weight: 700; color: #ffffff; background-color: #86868b; padding: 3px 8px; border-radius: 10px; text-transform: uppercase; margin-right: 5px; }");
        sb.append(".section-title { font-size: 11px; font-weight: 700; text-transform: uppercase; color: #86868b; margin: 24px 0 8px 0; letter-spacing: 0.5px; }");
        sb.append(".cta-button-wrapper { text-align: center; margin: 30px 0 10px 0; }");
        sb.append(".cta-button { display: inline-block; background-color: #0066cc; color: #ffffff !important; text-decoration: none; padding: 12px 24px; font-size: 14px; font-weight: 600; border-radius: 8px; transition: background-color 0.2s; }");
        sb.append(".cta-button:hover { background-color: #0052a3; }");
        sb.append(".footer { background: #f5f5f7; padding: 24px; text-align: center; border-top: 1px solid #e5e5ea; font-size: 12px; color: #86868b; }");
        sb.append(".footer a { color: #ff453a; text-decoration: none; font-weight: 500; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class='container'>");
        sb.append("  <div class='header'>");
        sb.append("    <h1>Interview Handbook</h1>");
        sb.append("    <p>매일 배달되는 오늘의 면접 챌린지 💡</p>");
        sb.append("  </div>");
        sb.append("  <div class='content'>");
        sb.append("    <span class='meta-tag'>" + question.getCategory() + "</span>");
        sb.append("    <span class='meta-tag'>" + question.getSubject() + "</span>");
        sb.append("    <h2 class='question-title'>" + question.getTitle() + "</h2>");
        
        String token = encryptionUtils.hash(toEmail);
        sb.append("    <div class='cta-button-wrapper'>");
        sb.append("      <a href='" + frontendUrl + "/handbook?questionId=" + question.getId() + "' class='cta-button' target='_blank'>정답 및 상세 해설 보기</a>");
        sb.append("    </div>");
        sb.append("  </div>");
        sb.append("  <div class='footer'>");
        sb.append("    <p>본 메일은 데일리 면접 질문 구독 서비스 신청자에 한해 발송됩니다.</p>");
        sb.append("    <p>구독을 원치 않으시면 언제든지 <a href='" + backendUrl + "/api/subscriptions/unsubscribe?token=" + token + "'>[여기서 구독 해제]</a> 하실 수 있습니다.</p>");
        sb.append("    <p>© 2026 Interview Handbook. All rights reserved.</p>");
        sb.append("  </div>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}
