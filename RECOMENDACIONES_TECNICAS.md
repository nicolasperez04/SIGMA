# RECOMENDACIONES TÉCNICAS DETALLADAS

## Cambios de Código Recomendados

### 1. Actualización de Dependencias

**Cambio en pom.xml:**

```xml
<!-- BUSCAR y REEMPLAZAR -->

<!-- ANTES (VULNERABLE) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- DESPUÉS (SEGURO) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.0</version>
</dependency>
```

**Verificar cambios:**
```bash
cd C:\Users\NICOLAS\Desktop\PROYECTO DE GRADO\BACKEND\SIGMA
mvn dependency:tree | grep poi-ooxml
# Resultado esperado: [INFO] org.apache.poi:poi-ooxml:jar:5.4.0
```

---

### 2. Mejorar Configuración de JWT

**Cambio en JwtService.java - Token Expiration:**

```java
// ANTES (5 minutos - MUY CORTO)
.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 300))

// DESPUÉS (30 minutos - MÁS REALISTA)
.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))

// O SI USAS REFRESH TOKENS (IDEAL)
.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))  // 15 min
```

**Considerar implementar Refresh Tokens:**

```java
// JwtService.java - Agregar método
public String generateRefreshToken(UserDetails userDetails) {
    return Jwts.builder()
        .setSubject(userDetails.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))  // 7 días
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

---

### 3. Mejorar SecurityConfig

**Agregar Protecciones Adicionales:**

```java
// SecurityConfig.java - Mejorado

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // ... código existente ...
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            
            // AGREGAR: Headers de seguridad
            .headers(headers -> headers
                .contentSecurityPolicy("default-src 'self'")
                .frameOptions(frameOptions -> frameOptions.deny())
                .xssProtection()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
            )
            
            // AGREGAR: Rate limiting (consideración futura)
            // .sessionManagement(session -> session
            //     .sessionFixationProtection(SessionFixationProtection.MIGRATEIZE_SESSION)
            // )
            
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        http.addFilterBefore(blackListFilter, 
            UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

### 4. Agregar Validación de Entrada

**Crear un DTO validator personalizado:**

```java
// src/main/java/com/SIGMA/USCO/common/validation/SigmaValidator.java
package com.SIGMA.USCO.common.validation;

import org.springframework.stereotype.Component;

@Component
public class SigmaValidator {
    
    public void validateEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Email inválido: " + email);
        }
    }
    
    public void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Contraseña debe contener mayúsculas");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Contraseña debe contener números");
        }
    }
    
    public void validatePhoneNumber(String phone) {
        if (!phone.matches("^\\+?[0-9]{7,15}$")) {
            throw new IllegalArgumentException("Número de teléfono inválido");
        }
    }
}
```

**Usar en AuthService:**

```java
@Service
public class AuthService {
    
    @Autowired
    private SigmaValidator validator;
    
    public ResponseEntity<?> register(AuthRequest request) {
        // Validar entrada
        validator.validateEmail(request.getEmail());
        validator.validatePasswordStrength(request.getPassword());
        
        // ... resto del código ...
    }
}
```

---

### 5. Agregar Logging Centralizado

**Mejorar logging en clases críticas:**

```java
// AuditLog.java - Crear entidad para auditoría
@Entity
@Table(name = "audit_logs")
@Slf4j
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime timestamp;
    
    @ManyToOne
    private User actor;
    
    private String action;
    
    private String entityType;
    
    private Long entityId;
    
    private String details;
    
    private String ipAddress;
    
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    private AuditStatus status;  // SUCCESS, FAILURE, DENIED
}

// AuditInterceptor.java
@Component
@Aspect
@Slf4j
public class AuditInterceptor {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Before("@annotation(Auditable)")
    public void auditBefore(JoinPoint joinPoint) {
        log.info("Auditing action: {}", joinPoint.getSignature().getName());
    }
    
    @AfterReturning("@annotation(Auditable)")
    public void auditAfterReturning(JoinPoint joinPoint) {
        String action = joinPoint.getSignature().getName();
        User actor = getCurrentUser();
        
        AuditLog log = AuditLog.builder()
            .timestamp(LocalDateTime.now())
            .actor(actor)
            .action(action)
            .status(AuditStatus.SUCCESS)
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();
        
        auditLogRepository.save(log);
        log.info("Audit recorded: {} by {}", action, actor.getUsername());
    }
}
```

**Usar en controladores:**

```java
@RestController
public class DocumentController {
    
    @PostMapping("/upload")
    @Auditable
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        // Acción será registrada automáticamente
        return ResponseEntity.ok("OK");
    }
}
```

---

### 6. Implementar Cache

**Mejorar performance con caching:**

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "modalities",
            "students",
            "programs",
            "documents"
        );
    }
}

// ModalityService.java - Usar cache
@Service
public class ModalityService {
    
    @Cacheable(value = "modalities", key = "#id")
    public StudentModality getModality(Long id) {
        return modalityRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Modalidad no encontrada"));
    }
    
    @CacheEvict(value = "modalities", key = "#id")
    public StudentModality updateModality(Long id, UpdateModalityRequest request) {
        // Actualizar y invalidar cache
        return modalityRepository.save(modality);
    }
}
```

---

### 7. Agregar Error Handling Centralizado

**Crear GlobalExceptionHandler:**

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
        EntityNotFoundException ex,
        HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
        Exception ex,
        HttpServletRequest request) {
        
        log.error("Unhandled exception", ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("Error interno del servidor")
            .path(request.getRequestURI())
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// ErrorResponse.java
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
}
```

---

## Configuraciones de Producción Recomendadas

### 1. PostgreSQL vs MySQL

**Consideración:** ¿Cambiar a PostgreSQL?

| Aspecto | MySQL 8.0 | PostgreSQL 15 |
|---------|-----------|---------------|
| Performance | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| ACID | ✅ (InnoDB) | ✅ |
| JSON Support | Básico | Avanzado |
| Full Text Search | ✅ | ✅ |
| Array Types | ❌ | ✅ |
| Replicación | ✅ | ✅ |
| Adopción | Alta | Creciente |

**Recomendación:** Mantener MySQL por ahora. Migrar a PostgreSQL solo si se necesitan features avanzadas.

---

### 2. Índices en Base de Datos

**Optimizar queries con índices:**

```sql
-- Crear índices en tablas críticas
CREATE INDEX idx_student_email ON users(email);
CREATE INDEX idx_modality_leader ON student_modality(leader_id);
CREATE INDEX idx_document_student ON student_document(student_id);
CREATE INDEX idx_history_status ON modality_process_status_history(status);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
```

**Verificar planes de ejecución:**
```sql
EXPLAIN SELECT * FROM student_modality WHERE leader_id = 123;
```

---

### 3. Backup Strategy

**Script de backup automático:**

```bash
#!/bin/bash
# backup.sh

DB_USER="sigma_user"
DB_PASS="$DB_PASSWORD"
DB_NAME="SIGMABD"
BACKUP_DIR="/backups/sigma"
DATE=$(date +"%Y%m%d_%H%M%S")

# Crear backup
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/sigma_$DATE.sql

# Comprimir
gzip $BACKUP_DIR/sigma_$DATE.sql

# Eliminar backups viejos (> 30 días)
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

# Subir a S3
aws s3 cp $BACKUP_DIR/sigma_$DATE.sql.gz s3://sigma-backups/

echo "Backup completado: sigma_$DATE.sql.gz"
```

**Agregar a crontab:**
```bash
crontab -e
# Ejecutar diariamente a las 2 AM
0 2 * * * /usr/local/bin/backup.sh >> /var/log/sigma-backup.log 2>&1
```

---

### 4. Monitoring y Alertas

**Configurar Prometheus + Grafana (opcional pero recomendado):**

```xml
<!-- pom.xml - Agregar -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
# application-prod.properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**Alertas importantes:**
- CPU > 80%
- Memoria > 85%
- Error rate > 1%
- Response time > 1000ms

---

## Plan de Migración a Producción

### Fase 1: Staging (1 semana)

```
Ambiente: Igual a producción, pero con datos de prueba
- ✅ Desplegar SIGMA en servidor de staging
- ✅ Ejecutar smoke tests
- ✅ Validar permisos y roles
- ✅ Pruebas de carga (simular 100 usuarios)
- ✅ Validación de reportes
```

### Fase 2: UAT (1-2 semanas)

```
Usuarios: Stakeholders reales
- ✅ Usar sistema completo
- ✅ Validar procesos académicos
- ✅ Feedback de UX
- ✅ Entrenamiento de usuarios
```

### Fase 3: Despliegue Azul-Verde (0 downtime)

```
Estrategia: Dos ambientes simultáneos
- ✅ Ambiente AZUL (actual)
- ✅ Ambiente VERDE (nuevo)
- ✅ Switch de tráfico sin downtime
- ✅ Rollback rápido si hay problemas
```

### Fase 4: Producción

```
Monitoreo 24/7:
- ✅ Alertas en tiempo real
- ✅ Equipo de soporte disponible
- ✅ Logs centralizados
- ✅ Métricas de performance
```

---

## Resumen de Comandos Maven Útiles

```bash
# Compilar sin tests
mvn clean compile

# Compilar y ejecutar tests
mvn clean verify

# Ejecutar tests con cobertura
mvn clean test jacoco:report

# Generar javadoc
mvn javadoc:javadoc

# Analizar código con SonarQube
mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000

# Ver dependencias
mvn dependency:tree

# Buscar vulnerabilidades
mvn org.owasp:dependency-check-maven:check

# Empaquetar JAR ejecutable
mvn package -DskipTests

# Ejecutar aplicación localmente
mvn spring-boot:run

# Generar proyecto de referencia
mvn archetype:generate
```

---

## Checklist Final Pre-Producción

```
SEGURIDAD
[ ] Todas las contraseñas en variables de entorno
[ ] JWT secret rotado y seguro
[ ] SSL/TLS configurado
[ ] CORS limitado a dominios conocidos
[ ] Swagger desactivado en producción
[ ] Logs sin información sensible

FUNCIONALIDAD
[ ] Todos los endpoints probados
[ ] Flujos críticos validados
[ ] Reportes generan correctamente
[ ] Email notifications funcionan
[ ] Archivos upload/download OK

PERFORMANCE
[ ] Load test OK (100+ usuarios)
[ ] Response time < 500ms
[ ] CPU utilization < 70%
[ ] Memory consumption stable
[ ] Database queries optimizadas

OPERACIONAL
[ ] Backups configurados y testeados
[ ] Logs centralizados
[ ] Monitoring y alertas activos
[ ] Plan de rollback documentado
[ ] Contatos de soporte disponibles

DOCUMENTACIÓN
[ ] README.md actualizado
[ ] API docs en Swagger
[ ] Runbook de operaciones
[ ] Contactos de emergencia
[ ] Calendarios de maintenance
```

---

**Preparado por:** GitHub Copilot  
**Fecha:** 18 de Abril de 2026  
**Versión:** 1.0


