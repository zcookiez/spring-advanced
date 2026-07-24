package org.example.expert.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // JwtFilter에서 request.setAttribute("userRole", ...)로 담아둔 권한 정보를 꺼내옵니다.
        String userRole = (String) request.getAttribute("userRole");

        // 유저 권한이 ADMIN이 아닌 경우 (권한이 없거나 다른 권한인 경우)
        if (userRole == null || !UserRole.ADMIN.name().equals(userRole)) {
            // 403 Forbidden 에러 코드를 내려보내고, 컨트롤러(API) 접근을 아예 차단합니다.
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 없습니다.");
            return false; // false를 반환하면 여기서 요청 처리가 멈춥니다.
        }

        // 유저 권한이 ADMIN이 맞다면, 아래처럼 요청 시간과 요청한 URL 주소를 로그로 남깁니다.
        log.info("Admin Access - Time: {}, URL: {}", LocalDateTime.now(), request.getRequestURI());
        
        return true; // true를 반환하면 다음 단계(컨트롤러 등)로 무사히 넘어갑니다.
    }
}
