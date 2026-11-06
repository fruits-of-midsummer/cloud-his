package com.cloudhis.common;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<?> notFound(Exception e) { return ResponseEntity.status(404).body(Map.of("message","资源不存在")); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> unsupportedMethod(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).allow(e.getSupportedHttpMethods()==null?new HttpMethod[0]:e.getSupportedHttpMethods().toArray(HttpMethod[]::new)).body(Map.of("message","该接口不支持此请求方法"));
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException e) { return ResponseEntity.status(e.status()).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getAllErrors().get(0).getDefaultMessage())); }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> bad(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","请求格式或参数不正确")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e) { return ResponseEntity.status(409).body(Map.of("message","记录已存在或关联数据已发生变化，请刷新后重试")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e, HttpServletRequest request) { String ref=UUID.randomUUID().toString().substring(0,8); log.error("Request {} failed [{}]",request.getRequestURI(),ref,e); return ResponseEntity.status(500).body(Map.of("message","操作未完成，请联系管理员，参考号："+ref)); }
}
