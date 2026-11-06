package com.cloudhis.service;

import com.cloudhis.common.*;
import com.cloudhis.mapper.AuthMapper;
import com.cloudhis.web.Requests;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static com.cloudhis.common.Values.*;

@Service
public class AuthService {
    public static final Set<String> ROLES=Set.of("ADMIN","CASHIER","DOCTOR","CHECK","LAB","TREAT","PHARMACY");
    private final AuthMapper mapper;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(10);
    private final ConcurrentHashMap<String,Attempt> attempts=new ConcurrentHashMap<>();
    private record Attempt(int failures,long since) {}
    public AuthService(AuthMapper mapper) { this.mapper=mapper; }
    public String encode(String password) {
        if(password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new BusinessException(400,"密码的UTF-8长度不能超过72字节");
        return encoder.encode(password);
    }
    public String csrf(HttpSession session) {
        if(session.getAttribute("csrf")==null) session.setAttribute("csrf",UUID.randomUUID().toString());
        return session.getAttribute("csrf").toString();
    }
    public Map<String,Object> current(HttpSession session) {
        Object id=session.getAttribute("userId");
        if(id==null) throw new BusinessException(401,"请先登录");
        Map<String,Object> user=mapper.byId(id.toString());
        if(user==null || !active(user)) { session.removeAttribute("userId"); throw new BusinessException(401,"账号已停用，请联系管理员"); }
        return user;
    }
    public Map<String,Object> require(HttpSession session,String... roles) {
        Map<String,Object> user=current(session);
        if(!str(user,"role_code").equals("ADMIN") && !Arrays.asList(roles).contains(str(user,"role_code"))) throw new BusinessException(403,"当前账号没有该操作权限");
        return user;
    }
    public Map<String,Object> login(Requests.Login body,HttpServletRequest request) {
        String key=request.getRemoteAddr()+":"+body.username(); long now=System.currentTimeMillis();
        attempts.entrySet().removeIf(e->now-e.getValue().since()>900000);
        Attempt attempt=attempts.get(key);
        if(attempt!=null && attempt.failures()>=8) throw new BusinessException(429,"登录失败次数过多，请15分钟后重试");
        Map<String,Object> user=mapper.byUsername(body.username());
        if(user==null || !encoder.matches(body.password(),str(user,"password_hash")) || !active(user)) {
            attempts.put(key,new Attempt(attempt==null?1:attempt.failures()+1,attempt==null?now:attempt.since()));
            throw new BusinessException(401,"账号或密码错误，或账号已停用");
        }
        attempts.remove(key); request.changeSessionId();
        request.getSession().setAttribute("userId",str(user,"id"));
        request.getSession().setAttribute("csrf",UUID.randomUUID().toString());
        audit(user,"LOGIN",str(user,"id"),"登录成功");
        return map("user",mapper.byId(str(user,"id")),"csrf_token",csrf(request.getSession()));
    }
    @Transactional public void changePassword(Map<String,Object> user,Requests.Password body) {
        if(!encoder.matches(body.oldPassword(),mapper.password(str(user,"id")))) throw new BusinessException(400,"原密码不正确");
        mapper.changePassword(str(user,"id"),encode(body.newPassword())); audit(user,"PASSWORD_CHANGE",str(user,"id"),"修改密码");
    }
    public void audit(Map<String,Object> user,String action,String entity,String detail) {
        mapper.audit(map("id",id(),"actor_id",str(user,"id"),"action_name",action,"entity_id",entity,"detail",detail));
    }
}
