package org.example.expert.config.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect      // 이 클래스가 AOP(관점 지향 프로그래밍) 설정을 담당한다는 것을 알려주는 어노테이션입니다.
@Component   // 스프링이 이 클래스를 빈(Bean)으로 등록하고 관리하도록 합니다.
@RequiredArgsConstructor
public class AdminLoggingAspect {

    // Java 객체를 JSON 문자열로 변환해 주는 ObjectMapper를 주입받습니다.
    private final ObjectMapper objectMapper;

    // @Pointcut: "어떤 메서드들이 실행될 때 가로챌 것인가?" 그 타겟(대상)을 지정한다.
    //  CommentAdminController의 deleteComment()
    //  UserAdminController의 changeUserRole()
    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..)) || " +
              "execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    public void adminApiMethods() {}

    // @Around: 대상 메서드가 "실행되기 전"과 "실행된 후" 모두에 끼어들어서 코드를 실행하겠다는 뜻입니다.
    @Around("adminApiMethods()")
    public Object logAdminApi(ProceedingJoinPoint joinPoint) throws Throwable {
        
        // 1. 스프링이 관리하는 컨텍스트에서 현재 들어온 HTTP 요청(HttpServletRequest) 객체를 억지로 꺼내옵니다.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 2. 요청 객체(request) 안에 JwtFilter가 미리 넣어둔 userId를 꺼내오고, URL과 현재 시간도 구합니다.
        Long userId = (Long) request.getAttribute("userId");
        String requestUrl = request.getRequestURI();
        LocalDateTime requestTime = LocalDateTime.now();

        // 3. 컨트롤러가 받은 파라미터(인자)들 중에서 진짜 HTTP 바디 데이터(@RequestBody)만 골라내는 작업
        Object requestBodyObj = null;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 가로챈 메서드의 정보
        Method method = signature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations(); // 각 파라미터에 붙은 어노테이션들
        Object[] args = joinPoint.getArgs(); // 실제 넘어온 파라미터 값들

        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                // 파라미터에 붙은 어노테이션 중 @RequestBody가 있다면, 그 값이 요청 본문
                if (annotation.annotationType().equals(RequestBody.class)) {
                    requestBodyObj = args[i];
                    break;
                }
            }
        }

        // 객체를 JSON 문자열로 예쁘게 변환한다. (비어있으면 "{}")
        String requestBodyJson = requestBodyObj != null ? objectMapper.writeValueAsString(requestBodyObj) : "{}";

        // --- [여기까지가 실제 API가 실행되기 '전'에 찍는 로그입니다] ---
        log.info("=== [Admin API Request] ===");
        log.info("Request Time : {}", requestTime);
        log.info("User ID      : {}", userId);
        log.info("Request URL  : {}", requestUrl);
        log.info("Request Body : {}", requestBodyJson);

        // 4. 이제 가로챘던 진짜 컨트롤러 메서드(API 로직)를 실행
        // 컨트롤러가 일을 다 마치고 클라이언트에게 돌려줄 **응답 데이터(반환값)**를 result로 받는다.
        Object result = joinPoint.proceed();

        // 5. 컨트롤러 메서드가 무사히 실행을 마치고 뱉어낸 결과물(반환값)을 JSON 문자열로 변환 (비어있으면 "{}")
        String responseBodyJson = result != null ? objectMapper.writeValueAsString(result) : "{}";

        // --- [여기는 실제 API가 실행된 '후' 로그] ---
        log.info("=== [Admin API Response] ===");
        log.info("Response Body: {}", responseBodyJson);

        // 마지막으로 원래 API가 돌려주려던 결과값을 그대로 사용자에게 반환
        return result;
    }
}
