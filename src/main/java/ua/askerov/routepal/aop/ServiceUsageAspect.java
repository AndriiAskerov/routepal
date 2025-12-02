package ua.askerov.routepal.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.service.ApiUsageAuditor;

@Aspect
@Component
public class ServiceUsageAspect {

    private final ApiUsageAuditor auditor;

    public ServiceUsageAspect(ApiUsageAuditor auditor) {
        this.auditor = auditor;
    }

    @Pointcut("execution(* ua.askerov.routepal.service.impl.RouteServiceImpl.calculateRoute(..))")
    public void routeServiceCall() {}

    // відповідний метод, що виконується навколо того, що відстежується
    @Around("routeServiceCall()")
    public Object auditRoute(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditor.tryIncrementDirectionsCounter()) {
            System.err.println("Ліміт Directions вичерпано");
            throw new RuntimeException("LIMIT_EXCEEDED");
        }
        return joinPoint.proceed();
    }

    @Pointcut("execution(* ua.askerov.routepal.service.ElevationService.getElevationForTrack(..))")
    public void elevationServiceCall() {}

    @Around("elevationServiceCall()")
    public Object auditElevation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditor.tryIncrementElevationCounter()) {
            System.err.println("Ліміт Elevation вичерпано");
            throw new RuntimeException("LIMIT_EXCEEDED");
        }
        return joinPoint.proceed();
    }
}