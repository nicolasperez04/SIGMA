# Documentación: Rúbricas Dinámicas de Evaluación Final (registerFinalDefenseEvaluation)

## 📋 Tabla de Contenidos
1. [Resumen de Cambios](#resumen-de-cambios)
2. [Contexto Técnico](#contexto-técnico)
3. [Arquitectura de Rúbricas](#arquitectura-de-rúbricas)
4. [Flujo de Evaluación](#flujo-de-evaluación)
5. [Guía de Payloads](#guía-de-payloads)
6. [Validaciones](#validaciones)
7. [Casos de Uso](#casos-de-uso)
8. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## Resumen de Cambios

### ¿Qué cambió?
Se refactorizó el método `registerFinalDefenseEvaluation()` para **soportar rúbricas de evaluación final dinámicas según la modalidad** en lugar de ser hardcodeado con los mismos 5 criterios para todas las modalidades.

### Cambios principales:
| Aspecto | Antes | Después |
|---------|-------|---------|
| **Criterios de evaluación** | 5 fijos (todos usan rúbrica estándar) | Dinámicos según modalidad |
| **Modalidad Estándar** | Dominio, Síntesis, Argumentación, Innovación, Presentación | ✓ Sin cambios |
| **Modalidad Emprendimiento** | Usa rúbrica estándar (incorrecto) | Usa 5 criterios específicos de emprendimiento |
| **Validación de criterios** | Todos obligatorios siempre | Valida solo criterios de la rúbrica esperada |
| **Compatibilidad hacia atrás** | N/A | ✓ Mantiene campos legacy para reportes |
| **Mapeo de datos** | N/A | Mapea criterios empresariales a campos legacy automáticamente |

### Archivos Modificados:

1. **`DefenseEvaluationCriteria.java`** (Entity)
   - ✅ Nuevo campo: `rubricType` (STANDARD | ENTREPRENEURSHIP)
   - ✅ 5 campos nuevos para criterios de emprendimiento
   - ✅ Los 5 campos estándar originales se mantienen

2. **`DefenseEvaluationCriteriaDTO.java`** (DTO entrada)
   - ✅ Nuevo campo: `rubricType` (opcional, el backend lo determina)
   - ✅ 5 campos nuevos para criterios de emprendimiento

3. **`FinalDefenseResponse.java`** (DTO respuesta)
   - ✅ Nuevo campo `rubricType` en `CriteriaDetail`
   - ✅ 5 campos nuevos para criterios de emprendimiento en respuesta

4. **`DefenseRubricType.java`** (Enum nuevo)
   ```java
   public enum DefenseRubricType {
       STANDARD,                // Rúbrica estándar (5 criterios)
       ENTREPRENEURSHIP         // Rúbrica de emprendimiento (5 criterios diferentes)
   }
   ```

5. **`ModalityService.java`** (Lógica de negocio)
   - ✅ Método `resolveDefenseRubricType()` - determina tipo de rúbrica por modalidad
   - ✅ Método `normalizeText()` - normaliza nombres de modalidades
   - ✅ Método `buildDefenseCriteriaResponse()` - serializa criterios dinámicamente
   - ✅ Método `buildFinalDefenseCriteriaDetail()` - construye respuesta final

---

## Contexto Técnico

### 🔍 ¿Cómo funciona internamente?

#### 1. **Resolución de Rúbrica**
Cuando un jurado registra su evaluación, el backend automáticamente:
```
1. Obtiene la modalidad del estudiante
2. Lee su nombre (ej: "Emprendimiento y fortalecimiento de empresa")
3. Normaliza el texto (minúsculas, sin acentos, espacios)
4. Compara con patrón conocido
5. Determina tipo de rúbrica: STANDARD o ENTREPRENEURSHIP
```

#### 2. **Validación de Criterios**
Según el tipo detectado:
- **STANDARD**: Valida que envíes los 5 criterios estándar
- **ENTREPRENEURSHIP**: Valida que envíes los 5 criterios de emprendimiento

Si falta alguno, devuelve error `400 Bad Request`.

#### 3. **Almacenamiento**
Se guarda todo en la misma tabla (`defense_evaluation_criteria`):
- Los 5 campos de rúbrica estándar (legacy)
- Los 5 campos de rúbrica de emprendimiento
- Un flag `rubric_type` que indica cuál se está usando

#### 4. **Mapeo Automático (Compatibilidad)**
Para mantener compatibilidad con sistemas antiguos que solo leen los 5 campos estándar:
```java
// Si es emprendimiento, mapea así:
domainAndClarity = entrepreneurshipCoherentBusinessObjectives
synthesisAndCommunication = entrepreneurshipPresentationSupportMaterial
argumentationAndResponse = entrepreneurshipDefenseSustentation
innovationAndImpact = entrepreneurshipAnalyticalCreativeCapacity
professionalPresentation = entrepreneurshipMethodologyTechnicalApproach
```

Este mapeo permite que queries/reportes legacy que leen `domain_and_clarity` sigan funcionando.

---

## Arquitectura de Rúbricas

### 📊 Rúbrica Estándar (STANDARD)

**Aplicable a**: Pasantía, Seminario, y otras modalidades no especificadas

| # | Criterio | Ponderación | Escala | Descripción |
|---|----------|------------|--------|-------------|
| 1 | Dominio del tema y claridad conceptual | 30% | I/A/B/E | Comprensión integral del problema y fundamentos teóricos |
| 2 | Capacidad de síntesis y comunicación oral | 15% | I/A/B/E | Exposición ordenada, coherente y ajustada al tiempo |
| 3 | Argumentación y capacidad de respuesta | 30% | I/A/B/E | Respuestas fundamentadas y seguras a preguntas |
| 4 | Innovación, impacto y aplicación del trabajo | 15% | I/A/B/E | Aportes, pertinencia e impacto |
| 5 | Presentación profesional y material de apoyo | 10% | I/A/B/E | Recursos audiovisuales y actitud profesional |

**Escala de valores**:
- `Insufficient` (I) → 0.0 - 2.9
- `Acceptable` (A) → 3.0 - 3.5
- `Good` (B) → 3.6 - 4.4
- `Excellent` (E) → 4.5 - 5.0

### 📊 Rúbrica Emprendimiento (ENTREPRENEURSHIP)

**Aplicable a**: "Emprendimiento y fortalecimiento de empresa"

| # | Criterio | Escala | Descripción |
|---|----------|--------|-------------|
| 1 | Presentación y manejo del material de apoyo | I/A/B/E | Uso efectivo de recursos audiovisuales y presentación |
| 2 | Formulación coherente de objetivos empresariales | I/A/B/E | Claridad y coherencia en objetivos del negocio |
| 3 | Metodología y enfoque técnico | I/A/B/E | Rigor técnico y metodológico del proyecto |
| 4 | Capacidad analítica con enfoque creativo | I/A/B/E | Análisis profundo con soluciones innovadoras |
| 5 | Defensa y sustentación de la propuesta empresarial | I/A/B/E | Argumentación sólida del modelo de negocio |

---

## Flujo de Evaluación

### 📍 Diagrama de Flujo

```
┌─────────────────────────────────────────────────────┐
│  Jurado intenta registrar evaluación final          │
│  POST /modalities/{modalityId}/final-defense-eval   │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│  ¿El jurado está asignado a esta modalidad?        │
│  ¿Ya registró evaluación antes?                     │
│  ¿La modalidad está en estado válido?               │
└──────────────────┬──────────────────────────────────┘
         NO        │        SÍ
         │         ▼
      ERROR    ┌─────────────────────────────────────┐
              │  Determinar tipo de rúbrica esperada │
              │  - Leer nombre de modalidad         │
              │  - Normalizar texto                 │
              │  - Comparar con patrones conocidos  │
              └──────────────┬──────────────────────┘
                             │
                  ┌──────────┴──────────┐
                  ▼                     ▼
          ┌────────────────┐  ┌──────────────────┐
          │    STANDARD    │  │  ENTREPRENEURSHIP│
          │   (Pasantía)   │  │  (Emprend.)      │
          └────────┬───────┘  └────────┬─────────┘
                   │                   │
      Valida 5    │                   │  Valida 5
      criterios   │                   │  criterios
      estándar    │                   │  empresariales
                  │                   │
                  └─────────┬─────────┘
                            ▼
         ┌──────────────────────────────────┐
         │  ¿Todos los criterios están      │
         │   presentes en el request?       │
         └──────────┬───────────────────────┘
            NO      │         SÍ
             │      ▼
          ERROR  ┌──────────────────────────┐
                │  Guardar DefenseEvaluation│
                │  Mapear campos (si aplica)│
                │  Procesar consenso jurados│
                └──────────┬───────────────┘
                           ▼
                ┌──────────────────────────┐
                │  Cambiar estado modalidad│
                │  según resultado consenso│
                │  APPROVED/REJECTED/etc   │
                └──────────┬───────────────┘
                           ▼
                    ┌────────────────┐
                    │  Retornar éxito│
                    │  con detalles  │
                    └────────────────┘
```

### 🔄 Estados de la modalidad durante evaluación

```
EXAMINERS_ASSIGNED
    ↓
READY_FOR_DEFENSE  (si todos docs finales aprobados)
    ↓
DEFENSE_SCHEDULED
    ↓
DEFENSE_COMPLETED (después de sustentación)
    ↓
UNDER_EVALUATION_PRIMARY_EXAMINERS
    ├─→ ¿Consenso?
    │   ├─ SÍ → GRADED_APPROVED o GRADED_FAILED
    │   └─ NO → DISAGREEMENT_REQUIRES_TIEBREAKER
    │             ↓
    │         UNDER_EVALUATION_TIEBREAKER
    │             ↓
    │         GRADED_APPROVED o GRADED_FAILED
```

---

## Guía de Payloads

### 📝 Endpoint

```
POST /api/modalities/{studentModalityId}/final-review-completed
```

### ✅ CASO 1: Modalidad ESTÁNDAR (Pasantía, Seminario, etc.)

**Solicitud:**
```json
{
  "grade": 4.2,
  "observations": "Excelente presentación del proyecto. Demuestra profundo conocimiento del tema.",
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

**Respuesta (éxito):**
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "grade": 4.2,
  "approved": true,
  "evaluationCriteria": {
    "id": 123,
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

### ✅ CASO 2: Modalidad EMPRENDIMIENTO

**Solicitud:**
```json
{
  "grade": 4.5,
  "observations": "Propuesta empresarial muy sólida con modelo de negocio innovador y sustentable.",
  "evaluationCriteria": {
    "entrepreneurshipPresentationSupportMaterial": "Excellent",
    "entrepreneurshipCoherentBusinessObjectives": "Excellent",
    "entrepreneurshipMethodologyTechnicalApproach": "Good",
    "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
    "entrepreneurshipDefenseSustentation": "Excellent",
    "proposedMention": "LAUREATE"
  }
}
```

**Respuesta (éxito):**
```json
{
  "success": true,
  "message": "Evaluación registrada correctamente. Esperando evaluación del otro jurado principal.",
  "grade": 4.5,
  "approved": true,
  "evaluationCriteria": {
    "id": 124,
    "rubricType": "ENTREPRENEURSHIP",
    "entrepreneurshipPresentationSupportMaterial": "Excellent",
    "entrepreneurshipCoherentBusinessObjectives": "Excellent",
    "entrepreneurshipMethodologyTechnicalApproach": "Good",
    "entrepreneurshipAnalyticalCreativeCapacity": "Excellent",
    "entrepreneurshipDefenseSustentation": "Excellent",
    "proposedMention": "LAUREATE",
    "evaluatedAt": "2025-03-13T14:35:00"
  }
}
```

### ❌ CASO 3: Error - Criterios Incompletos (STANDARD)

**Solicitud (INCOMPLETA):**
```json
{
  "grade": 3.8,
  "observations": "Buena presentación",
  "evaluationCriteria": {
    "domainAndClarity": "Good",
    "synthesisAndCommunication": "Good"
    // ❌ Faltan: argumentationAndResponse, innovationAndImpact, professionalPresentation
  }
}
```

**Respuesta (error):**
```json
{
  "success": false,
  "message": "Para esta modalidad debe enviar los 5 criterios estándar de la rúbrica.",
  "expectedRubricType": "STANDARD"
}
```

### ❌ CASO 4: Error - Tipo de Rúbrica No Coincide

**Solicitud (MISMATCH):**
```json
{
  "grade": 4.0,
  "observations": "Evaluación",
  "evaluationCriteria": {
    "rubricType": "ENTREPRENEURSHIP",  // ❌ Enviando emprendimiento
    "entrepreneurshipPresentationSupportMaterial": "Good",
    // ... otros criterios de emprendimiento
  }
}
```

**Respuesta (error):**
```json
{
  "success": false,
  "message": "El tipo de rúbrica enviado no coincide con la modalidad evaluada.",
  "expectedRubricType": "STANDARD",
  "receivedRubricType": "ENTREPRENEURSHIP"
}
```

---

## Validaciones

### ✔️ Validaciones Implementadas

#### 1. **Validación de Autorización**
```
- ¿El usuario autenticado es un jurado asignado a esta modalidad?
  → NO: Retorna 403 Forbidden
```

#### 2. **Validación de Evaluación Previa**
```
- ¿El jurado ya registró su evaluación antes?
  → SÍ: Retorna 400 Bad Request ("Ya ha registrado su evaluación")
```

#### 3. **Validación de Estado de Modalidad**
```
La modalidad debe estar en uno de estos estados:
- DEFENSE_COMPLETED
- READY_FOR_DEFENSE
- EXAMINERS_ASSIGNED
- UNDER_EVALUATION_PRIMARY_EXAMINERS
- UNDER_EVALUATION_TIEBREAKER
- DISAGREEMENT_REQUIRES_TIEBREAKER
- DEFENSE_SCHEDULED

Si NO está en uno de esos:
  → Retorna 400 Bad Request
```

#### 4. **Validación de Tipo de Jurado (Desempate)**
```
- Si es jurado de desempate: solo puede evaluar si hay desacuerdo
  (ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER)
  
- Si es jurado primario: no puede evaluar si ya hay desacuerdo resuelto
```

#### 5. **Validación de Calificación**
```
- La nota debe estar entre 0.0 y 5.0
  → NO: Retorna 400 Bad Request
```

#### 6. **Validación de Criterios (NUEVA - DINÁMICA)**

**Para STANDARD:**
```java
if (criteriaDTO.getDomainAndClarity() == null
    || criteriaDTO.getSynthesisAndCommunication() == null
    || criteriaDTO.getArgumentationAndResponse() == null
    || criteriaDTO.getInnovationAndImpact() == null
    || criteriaDTO.getProfessionalPresentation() == null) {
    // Error: Faltan criterios estándar
}
```

**Para ENTREPRENEURSHIP:**
```java
if (criteriaDTO.getEntrepreneurshipPresentationSupportMaterial() == null
    || criteriaDTO.getEntrepreneurshipCoherentBusinessObjectives() == null
    || criteriaDTO.getEntrepreneurshipMethodologyTechnicalApproach() == null
    || criteriaDTO.getEntrepreneurshipAnalyticalCreativeCapacity() == null
    || criteriaDTO.getEntrepreneurshipDefenseSustentation() == null) {
    // Error: Faltan criterios de emprendimiento
}
```

#### 7. **Validación de Tipo de Rúbrica (NUEVA)**
```
- Si el cliente envía rubricType: debe coincidir con el esperado
  → NO: Retorna 400 Bad Request con tipos esperado vs recibido
```

---

## Casos de Uso

### 🎯 Caso 1: Evaluar Pasantía (STANDARD)

**Contexto:**
- Estudiante: García López
- Modalidad: Pasantía
- Jurado: Dr. Martínez
- Calificación propuesta: 4.2/5

**Pasos:**
1. Jurado accede a `GET /api/modalities/{id}/defense-schedule` para ver detalles
2. Ve que es **modalidad ESTÁNDAR** (rúbrica con 5 criterios académicos)
3. Completa formulario con los 5 criterios estándar
4. Envía POST con evaluación

**Payload:**
```json
{
  "grade": 4.2,
  "observations": "Excelente desempeño en la pasantía. Demuestra madurez profesional y capacidad para resolver problemas complejos.",
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

**Resultado:**
- ✅ Evaluación guardada
- ✅ Sistema espera al segundo jurado
- ✅ Si segundo jurado también aprueba → Modalidad APROBADA

---

### 🎯 Caso 2: Evaluar Emprendimiento (ENTREPRENEURSHIP)

**Contexto:**
- Estudiante: Rodríguez Chen
- Modalidad: Emprendimiento y fortalecimiento de empresa
- Jurado: Dra. Gómez
- Propuesta: Plataforma de fintech para PyMEs
- Calificación propuesta: 4.8/5

**Pasos:**
1. Jurado accede a `GET /api/modalities/{id}/defense-schedule` para ver detalles
2. Ve que es **modalidad EMPRENDIMIENTO** (rúbrica empresarial con 5 criterios)
3. Completa formulario con los 5 criterios de emprendimiento
4. Envía POST con evaluación

**Payload:**
```json
{
  "grade": 4.8,
  "observations": "Propuesta empresarial excepcional. Modelo de negocio viable, innovador y con alto potencial de impacto. Análisis de mercado exhaustivo. Equipo bien preparado.",
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

**Resultado:**
- ✅ Evaluación guardada con tipo ENTREPRENEURSHIP
- ✅ Se mapean criterios a campos legacy automáticamente
- ✅ Sistema espera al segundo jurado
- ✅ Proposición de mención LAUREATE registrada

---

### 🎯 Caso 3: Desacuerdo entre Jurados

**Contexto:**
- Jurado 1: Aprueba (4.5/5)
- Jurado 2: Rechaza (2.8/5)
- Resultado: Se requiere jurado de desempate

**Pasos:**
1. Jurado 1 registra: `grade: 4.5` → Aprobado
2. Jurado 2 registra: `grade: 2.8` → Reprobado
3. Sistema detecta desacuerdo
4. Modalidad → `DISAGREEMENT_REQUIRES_TIEBREAKER`
5. Jurado 3 (desempate) puede ahora evaluar
6. Su decisión es definitiva

**Payload Jurado 3 (Desempate):**
```json
{
  "grade": 3.7,
  "observations": "La propuesta presenta aspectos positivos pero con limitaciones técnicas. Después de análisis detallado, considero que cumple los mínimos requeridos.",
  "evaluationCriteria": {
    "domainAndClarity": "Acceptable",
    "synthesisAndCommunication": "Good",
    "argumentationAndResponse": "Acceptable",
    "innovationAndImpact": "Acceptable",
    "professionalPresentation": "Good",
    "proposedMention": "NONE"
  }
}
```

**Resultado:**
- ✅ Desempate resuelto: APROBADO (3.7 >= 3.5)
- ✅ Sin distinción (proposedMention: NONE)
- ✅ Modalidad → `GRADED_APPROVED`

---

## Preguntas Frecuentes

### Q1: ¿Cómo sabe el sistema qué rúbrica usar?
**R:** Lee el nombre de la modalidad desde la base de datos. Si contiene "emprendimiento" (case-insensitive, sin acentos) → usa rúbrica ENTREPRENEURSHIP. Caso contrario → STANDARD.

### Q2: ¿Puedo enviar ambos tipos de criterios al mismo tiempo?
**R:** NO. El sistema espera SOLO los criterios del tipo que detecta. Si envías criterios de emprendimiento para una modalidad estándar, rechazará la solicitud.

### Q3: ¿Qué pasa con las evaluaciones antiguas (antes del cambio)?
**R:** Son compatibles. El campo `rubric_type` tiene valor por defecto STANDARD, así que queries antiguas seguirán funcionando sin cambios.

### Q4: ¿Se puede cambiar el nombre de la modalidad para cambiar la rúbrica?
**R:** No se recomienda. El sistema normaliza nombres (minúsculas, sin acentos) para la comparación. Si cambias el nombre exacto de "Emprendimiento y fortalecimiento de empresa", el sistema ya no reconocerá que es esa modalidad.

### Q5: ¿Qué sucede si falta la calificación (`grade`)?
**R:** Retorna error 400. El campo `grade` es OBLIGATORIO siempre, independientemente de la rúbrica.

### Q6: ¿Puedo proponer una mención si el estudiante reprueba?
**R:** NO. Las menciones (MERITORIOUS, LAUREATE) solo son válidas si la calificación es >= 3.5 (aprobado).

### Q7: ¿Cuántas veces puede intentar un jurado evaluar?
**R:** UNA sola vez. Si ya registró evaluación, intentar nuevamente retorna error "Ya ha registrado su evaluación para esta sustentación".

### Q8: ¿Qué sucede si el jurado de desempate reprueba?
**R:** La modalidad cambia a `GRADED_FAILED` y la distinción es `TIEBREAKER_REJECTED`.

### Q9: ¿Se pueden evaluar ambas rúbricas si agrego más campos?
**R:** NO. El sistema es mutuamente excluyente: O STANDARD O ENTREPRENEURSHIP. La rúbrica se determina automáticamente y se valida en consecuencia.

### Q10: ¿Cómo veo qué rúbrica se usó después de guardar?
**R:** En la respuesta GET de `/api/modalities/{id}/final-defense-result`, el campo `evaluationCriteria.rubricType` indicará "STANDARD" o "ENTREPRENEURSHIP".

---

## Notas Importantes

### ⚠️ Migración de Datos
Cuando despliegues estos cambios:
1. **Ejecutar migración SQL** para crear nuevas columnas en `defense_evaluation_criteria`:
   ```sql
   ALTER TABLE defense_evaluation_criteria 
   ADD COLUMN rubric_type VARCHAR(100) DEFAULT 'STANDARD',
   ADD COLUMN ent_presentation_support_material VARCHAR(100),
   ADD COLUMN ent_coherent_business_objectives VARCHAR(100),
   ADD COLUMN ent_methodology_technical_approach VARCHAR(100),
   ADD COLUMN ent_analytical_creative_capacity VARCHAR(100),
   ADD COLUMN ent_defense_sustentation VARCHAR(100);
   ```

2. **No requiere migración de datos existentes** (el default STANDARD mantiene compatibilidad)

### 🔄 Compatibilidad Hacia Atrás
- Queries antiguos que leen `domain_and_clarity`, `synthesis_and_communication`, etc. siguen funcionando
- Para emprendimiento, esos campos se rellenan automáticamente con mapeo de criterios empresariales

### 📊 Reporting
Para reports que distinga entre rúbricas:
```sql
SELECT 
  de.id,
  de.rubric_type,
  CASE 
    WHEN de.rubric_type = 'STANDARD' THEN de.domain_and_clarity
    WHEN de.rubric_type = 'ENTREPRENEURSHIP' THEN de.ent_coherent_business_objectives
  END AS criterion_1
FROM defense_evaluation_criteria de;
```

### 🔐 Validaciones en Frontend
Se recomienda en frontend:
1. Detectar modalidad antes de mostrar formulario
2. Mostrar SOLO los campos de la rúbrica correspondiente
3. Validar que todos los campos requeridos estén presentes antes de enviar

---

## Integración con Flujo Completo

```
FLUJO GENERAL DE DEFENSA:
├─ 1. Director programa asigna jurados
│   └─ assignExaminers() → EXAMINERS_ASSIGNED
│
├─ 2. Jefatura aprueba docs finales
│   └─ programHeadApprovesAndNotifiesExaminers() → READY_FOR_DEFENSE
│
├─ 3. Director programa propone fecha y lugar
│   └─ scheduleDefense() → DEFENSE_SCHEDULED
│
├─ 4. Sustentación se realiza
│   └─ Estado: DEFENSE_COMPLETED
│
└─ 5. Jurados registran evaluación ← AQUÍ ESTAMOS
    ├─ registerFinalDefenseEvaluation()
    │  ├─ Valida rúbrica dinámica
    │  ├─ Procesa criterios según tipo
    │  └─ Detecta consenso/desacuerdo
    │
    ├─ Si hay desacuerdo:
    │  └─ Jurado de desempate evalúa
    │
    └─ Resultado final: GRADED_APPROVED o GRADED_FAILED
```

---

**Documentación generada: 13 de Marzo de 2025**
**Versión: 1.0**
**Última actualización: Sistema de Rúbricas Dinámicas implementado**

