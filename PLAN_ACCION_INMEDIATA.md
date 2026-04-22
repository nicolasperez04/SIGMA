# ACCIONES INMEDIATAS - PLAN DE ACCIÓN

## 📋 Summary Ejecutivo

**Proyecto:** SIGMA  
**Estado:** Desarrollo Avanzado ✅  
**Readiness para Despliegue:** 62% ⚠️  
**Bloqueadores Críticos:** 5  
**Estimación de Tiempo para Producción:** 4-6 semanas

---

## 🔴 PROBLEMAS CRÍTICOS (Bloqueantes)

### 1. Vulnerabilidad de Seguridad - CVE-2025-31672
**Severidad:** MEDIUM | **Impacto:** Cyberataques potenciales  

**Problema:**
```xml
<!-- pom.xml - ACTUAL (VULNERABLE) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>  <!-- ← VULNERABLE -->
</dependency>
```

**Solución Inmediata:**
```xml
<!-- pom.xml - CORRECCIÓN -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.0</version>  <!-- ← SEGURO -->
</dependency>
```

**Pasos:**
1. Abrir `pom.xml`
2. Buscar `poi-ooxml`
3. Cambiar versión de `5.2.5` → `5.4.0`
4. Ejecutar `mvn clean package`
5. Validar que compila sin errores

---

### 2. Secretos Expuestos en Código
**Severidad:** CRÍTICA | **Impacto:** Compromiso de seguridad  

**Problemas actuales:**

```properties
# ❌ application-dev.properties - EXPUESTO
jwt.secret=E54791C31B99A58F25677B21FAECD57AB89CDEF1234567890ABCDEF12345678
spring.mail.password=oepyskbbflxgawri
spring.datasource.password=root
```

**Solución Inmediata:**

**Paso 1: Crear archivo .env para desarrollo**
```bash
# .env (NO commitear a git)
JWT_SECRET=E54791C31B99A58F25677B21FAECD57AB89CDEF1234567890ABCDEF12345678
MAIL_PASSWORD=oepyskbbflxgawri
DB_PASSWORD=root
DB_USERNAME=root
FRONTEND_URL=http://localhost:5173
```

**Paso 2: Modificar application-dev.properties**
```properties
jwt.secret=${JWT_SECRET:defaultSecret}
spring.mail.password=${MAIL_PASSWORD}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
frontend.url=${FRONTEND_URL:http://localhost:5173}
```

**Paso 3: Agregar a .gitignore**
```bash
# .gitignore
.env
.env.local
application-prod.properties
*.key
*.jks
```

**Paso 4: Si fue pusheado a Git (importante)**
```bash
# Cambiar secret en Git
git filter-branch --tree-filter 'sed -i "s/oepyskbbflxgawri/REMOVED/g" src/main/resources/application-dev.properties' HEAD

# O usar BFG Repo-Cleaner
bfg --replace-text passwords.txt
```

---

### 3. No existe Configuración de Producción
**Severidad:** CRÍTICA | **Impacto:** No hay base para desplegar  

**Solución: Crear application-prod.properties**

```properties
# src/main/resources/application-prod.properties

# ========== Base de Datos ==========
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:SIGMABD}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ========== JPA/Hibernate ==========
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate  # ← IMPORTANTE: validate, NO update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# ========== Encoding ==========
spring.http.encoding.charset=UTF-8
spring.http.encoding.enabled=true
spring.http.encoding.force=true

# ========== Multipart Upload ==========
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB

# ========== JWT ==========
jwt.secret=${JWT_SECRET}

# ========== Email ==========
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=8000
spring.mail.properties.mail.smtp.writetimeout=8000
spring.mail.default-encoding=UTF-8

# ========== File Storage ==========
file.upload-dir=${UPLOAD_DIR:/opt/sigma/uploads}

# ========== Frontend URL (CORS) ==========
frontend.url=${FRONTEND_URL}

# ========== Swagger/OpenAPI ==========
springdoc.swagger-ui.enabled=false  # ← IMPORTANTE: desactivar en producción
springdoc.api-docs.enabled=false
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs

# ========== SSL/TLS ==========
server.ssl.enabled=true
server.ssl.key-store=${KEYSTORE_PATH}
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat

# ========== Logging ==========
logging.level.root=INFO
logging.level.com.SIGMA=INFO
logging.level.org.springframework=WARN
logging.file.name=/var/log/sigma/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# ========== Server ==========
server.port=8443
server.servlet.context-path=/api
server.compression.enabled=true
server.compression.min-response-size=1024
```

**Pasos:**
1. Crear nuevo archivo: `src/main/resources/application-prod.properties`
2. Copiar contenido anterior
3. Configurar con variables de entorno
4. NO commitear con valores reales (usar placeholders)
5. En servidor: crear archivo real con valores

---

### 4. Cobertura de Tests Insuficiente
**Severidad:** IMPORTANTE | **Impacto:** Bugs en producción  

**Situación actual:**
```
Archivos de código: 274
Archivos de test: 4
Cobertura estimada: < 5%
Requerida para producción: ≥ 70%
```

**Acción Inmediata - Crear Tests Básicos:**

```java
// src/test/java/com/SIGMA/USCO/security/JwtServiceTest.java
package com.SIGMA.USCO.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {
    
    @Autowired
    private JwtService jwtService;
    
    private UserDetails testUser;
    
    @BeforeEach
    void setup() {
        testUser = new User(
            "test@example.com",
            "password",
            true,
            true,
            true,
            true,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }
    
    @Test
    void testGenerateToken_Success() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void testValidateToken_Success() {
        String token = jwtService.generateToken(testUser);
        boolean isValid = jwtService.validateToken(token, testUser);
        assertTrue(isValid);
    }
    
    @Test
    void testGetUsername_Success() {
        String token = jwtService.generateToken(testUser);
        String username = jwtService.getUsername(token);
        assertEquals("test@example.com", username);
    }
    
    @Test
    void testValidateToken_InvalidToken() {
        boolean isValid = jwtService.validateToken("invalid.token.here", testUser);
        assertFalse(isValid);
    }
}
```

**Crear más tests para:**
- Controladores (AuthController, DocumentController)
- Servicios (AuthService, ModalityService)
- Seguridad (SecurityConfig)
- Database (Repositories)

**Herramienta para medir cobertura:**
```bash
# Agregar a pom.xml
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
</dependency>

# Ejecutar y ver reporte
mvn clean test jacoco:report
# Ver en: target/site/jacoco/index.html
```

---

## 🟡 PROBLEMAS IMPORTANTES (Recomendados)

### 5. Crear archivo Dockerfile
**Para containerización y despliegue**

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copiar JAR generado
COPY target/SIGMA-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto
EXPOSE 8443

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8443/actuator/health || exit 1

# Variable de entorno para producción
ENV SPRING_PROFILES_ACTIVE=prod

# Ejecutar aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Crear docker-compose.yml para desarrollo:**

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Base de datos MySQL
  mysql:
    image: mysql:8.0
    container_name: sigma-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: SIGMABD
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - sigma-network

  # Aplicación SIGMA
  sigma-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: sigma-app
    ports:
      - "8443:8443"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/SIGMABD
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      FRONTEND_URL: http://localhost:5173
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      - mysql
    networks:
      - sigma-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8443/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3

volumes:
  mysql_data:

networks:
  sigma-network:
    driver: bridge
```

**Pasos:**
1. Crear `Dockerfile` en raíz
2. Crear `docker-compose.yml` en raíz
3. Probar: `docker-compose up`
4. Acceder: http://localhost:8443/swagger-ui.html

---

### 6. Crear Script de Despliegue
**Para automatizar deploy**

```bash
#!/bin/bash
# deploy.sh

set -e  # Exit on error

echo "=== SIGMA Deployment Script ==="

# Variables
ENVIRONMENT=${1:-prod}
REPO="sigma-prod"
IMAGE_NAME="sigma:latest"
CONTAINER_NAME="sigma-prod"
PORT="8443"

echo "Deploying to: $ENVIRONMENT"

# Step 1: Build
echo "Step 1: Building application..."
mvn clean package -DskipTests

# Step 2: Build Docker image
echo "Step 2: Building Docker image..."
docker build -t $IMAGE_NAME .

# Step 3: Stop old container
echo "Step 3: Stopping old container..."
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

# Step 4: Run new container
echo "Step 4: Starting new container..."
docker run -d \
  --name $CONTAINER_NAME \
  -p $PORT:8443 \
  --env-file .env.prod \
  --restart unless-stopped \
  $IMAGE_NAME

# Step 5: Health check
echo "Step 5: Health checking..."
sleep 5
curl http://localhost:$PORT/actuator/health || exit 1

echo "✅ Deployment successful!"
echo "Application available at: https://localhost:$PORT"
```

**Pasos:**
1. Crear archivo `deploy.sh`
2. Hacer ejecutable: `chmod +x deploy.sh`
3. Usar: `./deploy.sh prod`

---

### 7. Implementar Endpoint de Health Check
**Para monitoreo**

```java
// src/main/java/com/SIGMA/USCO/config/HealthCheckController.java
package com.SIGMA.USCO.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthCheckController {
    
    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }
}

@Component
class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public Health health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up()
                .withDetail("database", "MySQL")
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("status", e.getMessage())
                .build();
        }
    }
}
```

**Habilitar en properties:**
```properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true
```

---

## 🟢 MEJORAS DE LARGO PLAZO

### 8. Implementar CI/CD
**GitHub Actions o Jenkins**

```yaml
# .github/workflows/build.yml
name: Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean package
    
    - name: Run Tests
      run: mvn test
    
    - name: SonarQube Analysis
      run: mvn sonar:sonar -Dsonar.projectKey=SIGMA
    
    - name: Build Docker Image
      if: github.ref == 'refs/heads/main'
      run: docker build -t sigma:latest .
```

---

## 📋 CHECKLIST DE ACCIÓN INMEDIATA

```
SEMANA 1 - SEGURIDAD
[ ] Actualizar poi-ooxml a 5.4.0 en pom.xml
[ ] Mover secretos a variables de entorno
[ ] Crear application-prod.properties
[ ] Agregar archivos a .gitignore
[ ] Si necesario, limpiar Git history

SEMANA 2 - TESTING
[ ] Implementar tests unitarios (mínimo 70%)
[ ] Crear tests de integración
[ ] Configurar cobertura con JaCoCo
[ ] Load testing (100+ usuarios)

SEMANA 3 - DESPLIEGUE
[ ] Crear Dockerfile
[ ] Crear docker-compose.yml
[ ] Crear script deploy.sh
[ ] Implementar health checks
[ ] Configurar logging

SEMANA 4 - VALIDACIÓN
[ ] Testing completo en staging
[ ] Validación de seguridad
[ ] Monitoreo preparado
[ ] Plan de rollback

SEMANA 5 - PRODUCCIÓN
[ ] Crear servidor de producción
[ ] Configurar DNS y SSL
[ ] Despliegue inicial
[ ] Monitoreo 24/7
```

---

## 📞 Comandos Útiles

```bash
# Compilar proyecto
mvn clean package -DskipTests

# Ejecutar tests con cobertura
mvn clean test jacoco:report

# Ejecutar aplicación localmente
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Build Docker image
docker build -t sigma:latest .

# Run Docker container
docker run -d -p 8443:8443 --env-file .env sigma:latest

# Ver logs
docker logs -f sigma-prod

# Stop container
docker stop sigma-prod
```

---

## 🎯 Resumen

**Hoy (Crítico):**
1. ✅ Actualizar poi-ooxml (5 minutos)
2. ✅ Mover secretos a .env (15 minutos)
3. ✅ Crear application-prod.properties (20 minutos)

**Esta semana (Importante):**
4. ✅ Implementar 20 tests básicos (4 horas)
5. ✅ Crear Dockerfile (2 horas)
6. ✅ Crear docker-compose.yml (1 hora)

**Próximas 2 semanas (Recomendado):**
7. ✅ Completar cobertura de tests a 70% (8 horas)
8. ✅ Load testing (4 horas)
9. ✅ Seguridad adicional (4 horas)

**Total invertido:** ~2 semanas de desarrollo

---

**Preparado por:** GitHub Copilot  
**Fecha:** 18 de Abril de 2026


