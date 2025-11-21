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
public class L4_1_ServiceUsageAspect {

    private final ApiUsageAuditor auditor;

    public L4_1_ServiceUsageAspect(ApiUsageAuditor auditor) {
        this.auditor = auditor;
    }

    // методи, що відстежується
    @Pointcut("execution(* ua.askerov.routepal.service.impl.L4_2_RouteServiceImpl.calculateRoute(..))")
    public void routeServiceCall() {
    }

    // відповідні методи, що виконується навколо тих, що відстежуються
    @Around("routeServiceCall()")
    public Object auditApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditor.tryIncrementDirectionsCounter()) {
            // інформуємо клієнта, що ліміт вичерпано
            System.err.println("Перевищено ліміт запитів"); // DBG вивід на консоль TODO логування
            return RouteResponseDTO.builder()
                    .status("error")
                    .message(this.getClass().getSimpleName() + "Денний ліміт 'v2/directions вичерпано'. Спробуйте завтра").build();
        }
        // виклик самого методу (ліміт не вичерпано)
        return joinPoint.proceed();
    }
}