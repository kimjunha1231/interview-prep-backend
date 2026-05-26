package com.junha.interview.controller;

import com.junha.interview.common.ApiResponse;
import com.junha.interview.common.HtmlEscapeUtils;
import com.junha.interview.dto.subscription.SubscribeRequest;
import com.junha.interview.dto.subscription.UnsubscribeRequest;
import com.junha.interview.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@RequestBody @Valid SubscribeRequest request) {
        subscriptionService.subscribe(request.getEmail(), request.getCategory());
        return ApiResponse.success(null);
    }

    @PostMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribePost(@RequestBody @Valid UnsubscribeRequest request) {
        subscriptionService.unsubscribe(request.getEmail());
        return ApiResponse.success(null);
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public String unsubscribe(@RequestParam("token") String token) {
        try {
            subscriptionService.unsubscribeByHash(token);
            return buildHtmlPage("구독 취소 완료", "이메일 데일리 챌린지 구독이 성공적으로 취소되었습니다.", true);
        } catch (Exception e) {
            return buildHtmlPage("오류 발생", e.getMessage() != null ? e.getMessage() : "구독 취소 중 알 수 없는 에러가 발생했습니다.", false);
        }
    }

    private String buildHtmlPage(String title, String message, boolean success) {
        String color = success ? "#0066cc" : "#ff453a";
        String escapedTitle = HtmlEscapeUtils.escape(title);
        String escapedMessage = HtmlEscapeUtils.escape(message);
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>" + escapedTitle + "</title>" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Pretendard', sans-serif; background-color: #000000; color: #ffffff; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }" +
                ".card { background-color: #1c1c1e; border: 1px solid #2c2c2e; padding: 40px; border-radius: 12px; max-width: 480px; width: 100%; text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }" +
                "h1 { font-size: 24px; font-weight: 600; color: " + color + "; margin-top: 0; }" +
                "p { font-size: 15px; color: #86868b; line-height: 1.6; margin-bottom: 24px; }" +
                ".btn { display: inline-block; background-color: #0066cc; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 20px; font-weight: 500; font-size: 14px; transition: background-color 0.2s; }" +
                ".btn:hover { background-color: #0052a3; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='card'>" +
                "  <h1>" + escapedTitle + "</h1>" +
                "  <p>" + escapedMessage + "</p>" +
                "  <a href='" + frontendUrl + "' class='btn'>홈으로 이동</a>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

}
