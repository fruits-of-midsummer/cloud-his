package com.cloudhis.web;

import com.cloudhis.service.AuthService;
import com.cloudhis.common.BusinessException;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.cloudhis.common.Values.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @GetMapping("/session") Object session(HttpSession session) {
        Map<String,Object> user=null;
        try { user=auth.current(session); } catch(BusinessException ignored) {}
        return map("user",user,"csrf_token",auth.csrf(session));
    }
    @PostMapping("/login") Object login(@Valid @RequestBody Requests.Login body,HttpServletRequest request) { return auth.login(body,request); }
    @PostMapping("/logout") Object logout(HttpSession session) { auth.audit(auth.current(session),"LOGOUT","SESSION","退出登录"); session.invalidate(); return Map.of("ok",true); }
    @PostMapping("/password") Object password(@Valid @RequestBody Requests.Password body,HttpSession session) { auth.changePassword(auth.current(session),body); session.invalidate(); return Map.of("ok",true); }
}
