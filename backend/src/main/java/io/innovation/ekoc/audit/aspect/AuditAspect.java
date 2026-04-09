package io.innovation.ekoc.audit.aspect;

import io.innovation.ekoc.audit.annotation.Auditable;
import io.innovation.ekoc.audit.service.AuditService;
import io.innovation.ekoc.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(io.innovation.ekoc.audit.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String username = SecurityUtil.getCurrentUsername().orElse("anonymous");
        String resource = auditable.resource().isBlank()
                ? joinPoint.getSignature().getDeclaringType().getSimpleName()
                : auditable.resource();

        String resourceId = extractResourceId(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            auditService.log(username, auditable.action(), resource, resourceId, true, null, null);
            return result;
        } catch (Exception e) {
            auditService.log(username, auditable.action(), resource, resourceId, false, null, e.getMessage());
            throw e;
        }
    }

    private String extractResourceId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof UUID) return arg.toString();
            if (arg instanceof String s && isUuid(s)) return s;
        }
        return null;
    }

    private boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
