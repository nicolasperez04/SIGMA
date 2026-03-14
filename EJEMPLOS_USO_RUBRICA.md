# Guía Práctica: Ejemplos de Uso (Postman/cURL)

## Configuración General

### Endpoint Base
```
http://localhost:8080/api/modalities
```

### Headers Requeridos
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

### Formato General
```
POST /api/modalities/{studentModalityId}/final-review-completed
```

---

## 📚 EJEMPLOS PRÁCTICOS

## 1️⃣ EVALUAR MODALIDAD ESTÁNDAR (Pasantía)

### Escenario
- Estudiante: Juan Pérez (ID: 123)
- Modalidad: Pasantía
- Jurado: Dr. Carlos López
- Calificación: 4.2/5 (Buena)
- Mención propuesta: MERITORIOUS (Meritoria)

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 4.2,
    "observations": "Excelente desempeño en la pasantía. El estudiante demuestra profundo conocimiento de los conceptos trabajados, capacidad para comunicar ideas complejas de forma clara y respuestas muy bien fundamentadas a las preguntas formuladas.",
    "evaluationCriteria": {
      "domainAndClarity": "Excellent",
      "synthesisAndCommunication": "Good",
      "argumentationAndResponse": "Excellent",
      "innovationAndImpact": "Good",
      "professionalPresentation": "Excellent",
      "proposedMention": "MERITORIOUS"
    }
  }'
```

### JSON (para Postman)
```json
{
  "grade": 4.2,
  "observations": "Excelente desempeño en la pasantía. El estudiante demuestra profundo conocimiento de los conceptos trabajados, capacidad para comunicar ideas complejas de forma clara y respuestas muy bien fundamentadas a las preguntas formuladas.",
  "evaluationCriteria": {
    "domainAndClarity": "Excellent",
    "synthesisAndCommunication": "Good",
    "argumentationAndResponse": "Excellent",
    "innovationAndImpact": "Good",
    "professionalPresentation": "Excellent",
    "proposedMention": "MERITORIOUS"
  }
}
```

### Respuesta Esperada (200 OK)
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "hasConsensus": false,
  "requiresTiebreaker": false,
  "grade": 4.2,
  "approved": true,
  "evaluationCriteria": {
    "id": 1001,
    "rubricType": "STANDARD",
    "domainAndClarity": "Excellent",
    "synthesisAndCommunication": "Good",
    "argumentationAndResponse": "Excellent",
    "innovationAndImpact": "Good",
    "professionalPresentation": "Excellent",
    "proposedMention": "MERITORIOUS",
    "evaluatedAt": "2025-03-13T14:30:00"
  }
}
```

---

## 2️⃣ EVALUAR MODALIDAD EMPRENDIMIENTO

### Escenario
- Estudiante: María Rodríguez (ID: 789)
- Modalidad: Emprendimiento y fortalecimiento de empresa
- Proyecto: Plataforma de consultoría online
- Jurado: Dra. Sofía Gómez
- Calificación: 4.5/5 (Excelente)
- Mención propuesta: LAUREATE (Laureada)

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/789/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 4.5,
    "observations": "Propuesta empresarial excepcional. El plan de negocio es coherente, viable y cuenta con un análisis de mercado muy profundo. La innovación en el modelo está bien fundamentada. La defensa fue sólida, con respuestas precisas a todas las preguntas. Excelente potencial de impacto en el mercado.",
    "evaluationCriteria": {
      "entrepreneurshipPresentationSupportMaterial": "Excellent",
      "entrepreneurshipCoherentBusinessObjectives": "Excellent",
      "entrepreneurshipMethodologyTechnicalApproach": "Excellent",
      "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
      "entrepreneurshipDefenseSustentation": "Excellent",
      "proposedMention": "LAUREATE"
    }
  }'
```

### JSON (para Postman)
```json
{
  "grade": 4.5,
  "observations": "Propuesta empresarial excepcional. El plan de negocio es coherente, viable y cuenta con un análisis de mercado muy profundo. La innovación en el modelo está bien fundamentada. La defensa fue sólida, con respuestas precisas a todas las preguntas. Excelente potencial de impacto en el mercado.",
  "evaluationCriteria": {
    "entrepreneurshipPresentationSupportMaterial": "Excellent",
    "entrepreneurshipCoherentBusinessObjectives": "Excellent",
    "entrepreneurshipMethodologyTechnicalApproach": "Excellent",
    "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
    "entrepreneurshipDefenseSustentation": "Excellent",
    "proposedMention": "LAUREATE"
  }
}
```

### Respuesta Esperada (200 OK)
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "hasConsensus": false,
  "requiresTiebreaker": false,
  "grade": 4.5,
  "approved": true,
  "evaluationCriteria": {
    "id": 1002,
    "rubricType": "ENTREPRENEURSHIP",
    "entrepreneurshipPresentationSupportMaterial": "Excellent",
    "entrepreneurshipCoherentBusinessObjectives": "Excellent",
    "entrepreneurshipMethodologyTechnicalApproach": "Excellent",
    "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
    "entrepreneurshipDefenseSustentation": "Excellent",
    "proposedMention": "LAUREATE",
    "evaluatedAt": "2025-03-13T14:35:00"
  }
}
```

---

## 3️⃣ EVALUAR CON CALIFICACIÓN BAJA (Rechazo)

### Escenario
- Estudiante: Pedro García (ID: 555)
- Modalidad: Seminario
- Jurado: Ing. Roberto López
- Calificación: 2.8/5 (Reprobado)

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/555/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 2.8,
    "observations": "La presentación carece de profundidad en el análisis del tema. Aunque se identifica claramente el problema, las soluciones propuestas no están suficientemente justificadas. El estudiante no logró responder adecuadamente a las preguntas sobre los aspectos técnicos. Se recomienda reforzar el análisis antes de una próxima presentación.",
    "evaluationCriteria": {
      "domainAndClarity": "Acceptable",
      "synthesisAndCommunication": "Acceptable",
      "argumentationAndResponse": "Insufficient",
      "innovationAndImpact": "Insufficient",
      "professionalPresentation": "Acceptable",
      "proposedMention": "NONE"
    }
  }'
```

### JSON (para Postman)
```json
{
  "grade": 2.8,
  "observations": "La presentación carece de profundidad en el análisis del tema. Aunque se identifica claramente el problema, las soluciones propuestas no están suficientemente justificadas. El estudiante no logró responder adecuadamente a las preguntas sobre los aspectos técnicos. Se recomienda reforzar el análisis antes de una próxima presentación.",
  "evaluationCriteria": {
    "domainAndClarity": "Acceptable",
    "synthesisAndCommunication": "Acceptable",
    "argumentationAndResponse": "Insufficient",
    "innovationAndImpact": "Insufficient",
    "professionalPresentation": "Acceptable",
    "proposedMention": "NONE"
  }
}
```

### Respuesta Esperada (200 OK)
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "hasConsensus": false,
  "requiresTiebreaker": false,
  "grade": 2.8,
  "approved": false,
  "evaluationCriteria": {
    "id": 1003,
    "rubricType": "STANDARD",
    "domainAndClarity": "Acceptable",
    "synthesisAndCommunication": "Acceptable",
    "argumentationAndResponse": "Insufficient",
    "innovationAndImpact": "Insufficient",
    "professionalPresentation": "Acceptable",
    "proposedMention": "NONE",
    "evaluatedAt": "2025-03-13T14:40:00"
  }
}
```

---

## 4️⃣ ERROR - CRITERIOS INCOMPLETOS

### Escenario (Incorrecto)
Intentar enviar evaluación de STANDARD sin todos los 5 criterios

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 4.0,
    "observations": "Buena presentación",
    "evaluationCriteria": {
      "domainAndClarity": "Good",
      "synthesisAndCommunication": "Good"
    }
  }'
```

### Respuesta Esperada (400 Bad Request)
```json
{
  "success": false,
  "message": "Para esta modalidad debe enviar los 5 criterios estándar de la rúbrica.",
  "expectedRubricType": "STANDARD"
}
```

---

## 5️⃣ ERROR - TIPO DE RÚBRICA NO COINCIDE

### Escenario (Incorrecto)
Intentar enviar criterios de ENTREPRENEURSHIP para modalidad STANDARD

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 4.2,
    "observations": "Evaluación",
    "evaluationCriteria": {
      "rubricType": "ENTREPRENEURSHIP",
      "entrepreneurshipPresentationSupportMaterial": "Excellent",
      "entrepreneurshipCoherentBusinessObjectives": "Excellent",
      "entrepreneurshipMethodologyTechnicalApproach": "Good",
      "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
      "entrepreneurshipDefenseSustentation": "Excellent",
      "proposedMention": "LAUREATE"
    }
  }'
```

### Respuesta Esperada (400 Bad Request)
```json
{
  "success": false,
  "message": "El tipo de rúbrica enviado no coincide con la modalidad evaluada.",
  "expectedRubricType": "STANDARD",
  "receivedRubricType": "ENTREPRENEURSHIP"
}
```

---

## 6️⃣ ERROR - CALIFICACIÓN FUERA DE RANGO

### Escenario (Incorrecto)
Enviar calificación > 5.0

### cURL
```bash
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "grade": 5.5,
    "observations": "Excelente",
    "evaluationCriteria": {
      "domainAndClarity": "Excellent",
      "synthesisAndCommunication": "Good",
      "argumentationAndResponse": "Excellent",
      "innovationAndImpact": "Good",
      "professionalPresentation": "Excellent",
      "proposedMention": "NONE"
    }
  }'
```

### Respuesta Esperada (400 Bad Request)
```json
{
  "success": false,
  "message": "La calificación debe estar entre 0.0 y 5.0"
}
```

---

## 7️⃣ ERROR - YA EVALUÓ ANTES

### Escenario (Incorrecto)
Mismo jurado intenta evaluar dos veces

### cURL
```bash
# Primera evaluación: ✅ ÉXITO
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JURADO_TOKEN}" \
  -d '{ ... }'  # → Respuesta: 200 OK

# Segunda evaluación del MISMO jurado: ❌ ERROR
curl -X POST "http://localhost:8080/api/modalities/456/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JURADO_TOKEN}" \
  -d '{ ... }'
```

### Respuesta Esperada (400 Bad Request)
```json
{
  "success": false,
  "message": "Ya ha registrado su evaluación para esta sustentación"
}
```

---

## 8️⃣ ESCENARIO: DESACUERDO ENTRE JURADOS

### Situación
- **Jurado 1** evalúa y aprueba (4.5/5)
- **Jurado 2** evalúa y reprueba (2.9/5)
- Sistema detecta desacuerdo

### Jurado 1: Aprobación
```bash
curl -X POST "http://localhost:8080/api/modalities/999/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JURADO1_TOKEN}" \
  -d '{
    "grade": 4.5,
    "observations": "Excelente trabajo. Muy bien sustentado.",
    "evaluationCriteria": {
      "domainAndClarity": "Excellent",
      "synthesisAndCommunication": "Excellent",
      "argumentationAndResponse": "Excellent",
      "innovationAndImpact": "Good",
      "professionalPresentation": "Excellent",
      "proposedMention": "MERITORIOUS"
    }
  }'
```

**Respuesta Jurado 1:**
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "grade": 4.5,
  "approved": true,
  "hasConsensus": false
}
```

### Jurado 2: Rechazo
```bash
curl -X POST "http://localhost:8080/api/modalities/999/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JURADO2_TOKEN}" \
  -d '{
    "grade": 2.9,
    "observations": "No demuestra dominio suficiente del tema. Las respuestas fueron superficiales.",
    "evaluationCriteria": {
      "domainAndClarity": "Acceptable",
      "synthesisAndCommunication": "Acceptable",
      "argumentationAndResponse": "Insufficient",
      "innovationAndImpact": "Insufficient",
      "professionalPresentation": "Good",
      "proposedMention": "NONE"
    }
  }'
```

**Respuesta Jurado 2:**
```json
{
  "success": true,
  "message": "No hay consenso entre los jurados principales. Se requiere asignar un tercer jurado para desempatar.",
  "hasConsensus": false,
  "requiresTiebreaker": true,
  "status": "DISAGREEMENT_REQUIRES_TIEBREAKER"
}
```

### Jurado 3 (Desempate): Decisión Final
El jurado de desempate ahora puede evaluar y su decisión es definitiva.

```bash
curl -X POST "http://localhost:8080/api/modalities/999/final-review-completed" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JURADO3_TOKEN}" \
  -d '{
    "grade": 3.6,
    "observations": "Después de analizar las perspectivas de ambos jurados, considero que el trabajo presenta aspectos positivos aunque con limitaciones. La sustentación fue aceptable y el estudiante logra demostrar competencia en el dominio.",
    "evaluationCriteria": {
      "domainAndClarity": "Good",
      "synthesisAndCommunication": "Good",
      "argumentationAndResponse": "Acceptable",
      "innovationAndImpact": "Acceptable",
      "professionalPresentation": "Good",
      "proposedMention": "NONE"
    }
  }'
```

**Respuesta Jurado 3 (Desempate):**
```json
{
  "success": true,
  "message": "Modalidad APROBADA por decisión del jurado de desempate",
  "isTiebreaker": true,
  "finalStatus": "GRADED_APPROVED",
  "finalGrade": 3.6,
  "academicDistinction": "TIEBREAKER_APPROVED"
}
```

**Resultado Final:**
- ✅ Modalidad aprobada (3.6 >= 3.5)
- ✅ Sin distinción honorífica
- ✅ Estado: GRADED_APPROVED
- ✅ Notas guardadas en historial

---

## ⚙️ COLECCIÓN POSTMAN COMPLETA

```json
{
  "info": {
    "name": "Evaluación Final de Defensa (Rúbricas Dinámicas)",
    "description": "Colección de ejemplos para registrar evaluación final de sustentación",
    "version": "1.0"
  },
  "item": [
    {
      "name": "1. Evaluar STANDARD - Pasantía (Aprobado)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"grade\": 4.2,\n  \"observations\": \"Excelente desempeño en la pasantía.\",\n  \"evaluationCriteria\": {\n    \"domainAndClarity\": \"Excellent\",\n    \"synthesisAndCommunication\": \"Good\",\n    \"argumentationAndResponse\": \"Excellent\",\n    \"innovationAndImpact\": \"Good\",\n    \"professionalPresentation\": \"Excellent\",\n    \"proposedMention\": \"MERITORIOUS\"\n  }\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/modalities/{{modalityId}}/final-review-completed",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "modalities", "{{modalityId}}", "final-review-completed"]
        }
      }
    },
    {
      "name": "2. Evaluar ENTREPRENEURSHIP - Emprendimiento (Aprobado)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"grade\": 4.5,\n  \"observations\": \"Propuesta empresarial excepcional.\",\n  \"evaluationCriteria\": {\n    \"entrepreneurshipPresentationSupportMaterial\": \"Excellent\",\n    \"entrepreneurshipCoherentBusinessObjectives\": \"Excellent\",\n    \"entrepreneurshipMethodologyTechnicalApproach\": \"Excellent\",\n    \"entrepreneurshipAnalyticalCreativeCapacity\": \"Excellent\",\n    \"entrepreneurshipDefenseSustentation\": \"Excellent\",\n    \"proposedMention\": \"LAUREATE\"\n  }\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/modalities/{{modalityId}}/final-review-completed",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "modalities", "{{modalityId}}", "final-review-completed"]
        }
      }
    },
    {
      "name": "3. Evaluar STANDARD - Reprobado",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"grade\": 2.8,\n  \"observations\": \"La presentación carece de profundidad.\",\n  \"evaluationCriteria\": {\n    \"domainAndClarity\": \"Acceptable\",\n    \"synthesisAndCommunication\": \"Acceptable\",\n    \"argumentationAndResponse\": \"Insufficient\",\n    \"innovationAndImpact\": \"Insufficient\",\n    \"professionalPresentation\": \"Acceptable\",\n    \"proposedMention\": \"NONE\"\n  }\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/modalities/{{modalityId}}/final-review-completed",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "modalities", "{{modalityId}}", "final-review-completed"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "token",
      "value": "tu_jwt_token_aqui"
    },
    {
      "key": "modalityId",
      "value": "456"
    }
  ]
}
```

---

## 📌 RESUMEN RÁPIDO

| Modalidad | Criterios a Enviar | Ejemplo | Ubicación |
|-----------|-----------------|---------|-----------|
| **STANDARD** (Pasantía, Seminario, etc.) | `domainAndClarity`, `synthesisAndCommunication`, `argumentationAndResponse`, `innovationAndImpact`, `professionalPresentation` | Criterios académicos clásicos | [Ejemplo 1](#1️⃣-evaluar-modalidad-estándar-pasantía) |
| **ENTREPRENEURSHIP** (Emprendimiento y fortalecimiento de empresa) | `entrepreneurshipPresentationSupportMaterial`, `entrepreneurshipCoherentBusinessObjectives`, `entrepreneurshipMethodologyTechnicalApproach`, `entrepreneurshipAnalyticalCreativeCapacity`, `entrepreneurshipDefenseSustentation` | Criterios empresariales | [Ejemplo 2](#2️⃣-evaluar-modalidad-emprendimiento) |

---

**Última actualización: 13 de Marzo de 2025**

