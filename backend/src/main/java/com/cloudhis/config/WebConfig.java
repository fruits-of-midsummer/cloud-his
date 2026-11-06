package com.cloudhis.config;

import com.cloudhis.common.BusinessException;
import com.cloudhis.service.AuthService;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthService auth;
    public WebConfig(AuthService auth) { this.auth=auth; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                response.setHeader("X-Content-Type-Options","nosniff");
                response.setHeader("X-Frame-Options","DENY");
                response.setHeader("Referrer-Policy","same-origin");
                response.setHeader("Cache-Control","no-store");
                String uri=request.getRequestURI();
                if(!uri.startsWith("/api/") || uri.equals("/api/health")) return true;
                HttpSession session=request.getSession();
                if(!request.getMethod().equals("GET")) {
                    String token=request.getHeader("X-CSRF-Token");
                    if(token==null || !MessageDigest.isEqual(auth.csrf(session).getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))) throw new BusinessException(403,"页面会话已变化，请刷新页面重试");
                }
                if(!uri.equals("/api/auth/session") && !uri.equals("/api/auth/login")) auth.current(session);
                return true;
            }
        });
    }
}
