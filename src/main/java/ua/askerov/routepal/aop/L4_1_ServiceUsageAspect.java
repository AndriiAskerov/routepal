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

    // --- 1. Аудит розрахунку маршруту (як було) ---
    @Pointcut("execution(* ua.askerov.routepal.service.impl.L4_2_RouteServiceImpl.calculateRoute(..))")
    public void routeServiceCall() {}

    // відповідний метод, що виконується навколо того, що відстежується
    @Around("routeServiceCall()")
    public Object auditRoute(ProceedingJoinPoint joinPoint) throws Throwable {
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

    // --- 2. НОВЕ: Аудит експорту (цілимося в ExportService) ---
    // Можна вказати інтерфейс або реалізацію
    @Pointcut("execution(* ua.askerov.routepal.service.ExportService.exportRoute(..))")
    public void exportServiceCall() {}

    @Around("exportServiceCall()")
    public Object auditExport(ProceedingJoinPoint joinPoint) throws Throwable {
        // Перевіряємо ІНШИЙ лічильник
        if (!auditor.tryIncrementExportCounter()) {
            System.err.println("Ліміт Export вичерпано");
            // Кидаємо виняток, який перехопить контролер
            throw new RuntimeException("LIMIT_EXCEEDED");
        }
        return joinPoint.proceed();
    }
}