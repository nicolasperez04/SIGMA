# Guía de Integración Frontend - Rúbricas Dinámicas

## 📱 Implementación en Frontend

### Overview
El frontend debe detectar automáticamente el tipo de rúbrica según la modalidad y mostrar el formulario correspondiente.

---

## 🎯 FLUJO DE USUARIO EN FRONTEND

```
┌─────────────────────────────────────────────────┐
│  Jurado accede a su panel de evaluación        │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
     ┌───────────────────────────┐
     │ Obtiene lista de          │
     │ sustentaciones asignadas  │
     │ GET /api/modalities/...   │
     └───────────┬───────────────┘
                 │
                 ▼
     ┌───────────────────────────────┐
     │ Selecciona sustentación       │
     │ Lee: modalidad.name           │
     └───────────┬───────────────────┘
                 │
                 ▼
     ┌───────────────────────────────┐
     │ Determina tipo de rúbrica     │
     │ Frontend verifica:            │
     │ if (name.includes('emprend')) │
     │   → ENTREPRENEURSHIP          │
     │ else → STANDARD               │
     └───────────┬───────────────────┘
                 │
    ┌────────────┴────────────┐
    ▼                         ▼
┌──────────────┐      ┌──────────────────┐
│  STANDARD    │      │  ENTREPRENEURSHIP│
│  Formulario  │      │  Formulario      │
│  5 campos    │      │  5 campos        │
│  académicos  │      │  empresariales   │
└──────┬───────┘      └────────┬─────────┘
       │                       │
       │ Completa y envía      │ Completa y envía
       │ POST /api/modalities/ │ POST /api/modalities/
       │     .../final-...     │     .../final-...
       │                       │
       └───────────┬───────────┘
                   ▼
        ┌──────────────────────┐
        │  Respuesta: 200 OK   │
        │  evaluationCriteria  │
        │  → rubricType        │
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │ Mostrar confirmación │
        │ "Evaluación guardada"│
        └──────────────────────┘
```

---

## 💻 CÓDIGO FRONTEND (Pseudocódigo)

### 1. Servicio Angular/React para Detectar Rúbrica

```typescript
// rubricService.ts
export class RubricService {
  
  // Detectar tipo de rúbrica según nombre de modalidad
  detectRubricType(modalityName: string): 'STANDARD' | 'ENTREPRENEURSHIP' {
    // Normalizar: minúsculas, sin acentos
    const normalized = this.normalizeText(modalityName);
    
    if (normalized.includes('emprendimiento')) {
      return 'ENTREPRENEURSHIP';
    }
    return 'STANDARD';
  }
  
  // Normalizar texto (quitar acentos, minúsculas)
  private normalizeText(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')  // Remover diacríticos
      .trim();
  }
  
  // Obtener campos requeridos según tipo de rúbrica
  getRequiredFields(rubricType: 'STANDARD' | 'ENTREPRENEURSHIP'): string[] {
    if (rubricType === 'ENTREPRENEURSHIP') {
      return [
        'entrepreneurshipPresentationSupportMaterial',
        'entrepreneurshipCoherentBusinessObjectives',
        'entrepreneurshipMethodologyTechnicalApproach',
        'entrepreneurshipAnalyticalCreativeCapacity',
        'entrepreneurshipDefenseSustentation'
      ];
    }
    
    return [
      'domainAndClarity',
      'synthesisAndCommunication',
      'argumentationAndResponse',
      'innovationAndImpact',
      'professionalPresentation'
    ];
  }
  
  // Obtener etiquetas para campos (para mostrar en UI)
  getFieldLabels(rubricType: 'STANDARD' | 'ENTREPRENEURSHIP'): Record<string, string> {
    if (rubricType === 'ENTREPRENEURSHIP') {
      return {
        'entrepreneurshipPresentationSupportMaterial': 
          '1. Presentación y manejo del material de apoyo',
        'entrepreneurshipCoherentBusinessObjectives': 
          '2. Formulación coherente de objetivos empresariales',
        'entrepreneurshipMethodologyTechnicalApproach': 
          '3. Metodología y enfoque técnico',
        'entrepreneurshipAnalyticalCreativeCapacity': 
          '4. Capacidad analítica con enfoque creativo',
        'entrepreneurshipDefenseSustentation': 
          '5. Defensa y sustentación de la propuesta empresarial'
      };
    }
    
    return {
      'domainAndClarity': 
        '1. Dominio del tema y claridad conceptual (30%)',
      'synthesisAndCommunication': 
        '2. Capacidad de síntesis y comunicación oral (15%)',
      'argumentationAndResponse': 
        '3. Argumentación y capacidad de respuesta (30%)',
      'innovationAndImpact': 
        '4. Innovación, impacto y aplicación (15%)',
      'professionalPresentation': 
        '5. Presentación profesional y material (10%)'
    };
  }
}
```

### 2. Componente React para Formulario de Evaluación

```jsx
// FinalDefenseEvaluationForm.jsx
import React, { useState, useEffect } from 'react';
import { RubricService } from './rubricService';

export const FinalDefenseEvaluationForm = ({ modalityId, modalityName }) => {
  const rubricService = new RubricService();
  
  const [rubricType, setRubricType] = useState(null);
  const [formData, setFormData] = useState({
    grade: 3.5,
    observations: '',
    evaluationCriteria: {}
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    // Detectar tipo de rúbrica
    const detected = rubricService.detectRubricType(modalityName);
    setRubricType(detected);
  }, [modalityName]);

  const handleCriteriaChange = (fieldName, value) => {
    setFormData(prev => ({
      ...prev,
      evaluationCriteria: {
        ...prev.evaluationCriteria,
        [fieldName]: value
      }
    }));
  };

  const validateForm = () => {
    const requiredFields = rubricService.getRequiredFields(rubricType);
    
    for (let field of requiredFields) {
      if (!formData.evaluationCriteria[field]) {
        setError(`El criterio "${field}" es obligatorio`);
        return false;
      }
    }
    
    if (!formData.grade || formData.grade < 0 || formData.grade > 5) {
      setError('La calificación debe estar entre 0.0 y 5.0');
      return false;
    }
    
    if (!formData.observations || formData.observations.trim().length === 0) {
      setError('Las observaciones son obligatorias');
      return false;
    }
    
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/modalities/${modalityId}/final-review-completed`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify(formData)
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al enviar evaluación');
      }

      const result = await response.json();
      setSuccess(true);
      
      // Mostrar confirmación
      setTimeout(() => {
        window.location.reload();
      }, 2000);
      
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (!rubricType) {
    return <div>Cargando...</div>;
  }

  const requiredFields = rubricService.getRequiredFields(rubricType);
  const fieldLabels = rubricService.getFieldLabels(rubricType);

  return (
    <form onSubmit={handleSubmit} className="evaluation-form">
      <h2>
        Evaluación Final - {rubricType === 'ENTREPRENEURSHIP' 
          ? 'Rúbrica Empresarial' 
          : 'Rúbrica Estándar'}
      </h2>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">Evaluación guardada exitosamente</div>}

      {/* Calificación */}
      <div className="form-group">
        <label htmlFor="grade">Calificación (0.0 - 5.0):</label>
        <input
          type="number"
          id="grade"
          min="0"
          max="5"
          step="0.1"
          value={formData.grade}
          onChange={(e) => setFormData({ ...formData, grade: parseFloat(e.target.value) })}
          required
        />
      </div>

      {/* Criterios de Rúbrica */}
      <div className="rubric-section">
        <h3>Criterios de Evaluación</h3>
        {requiredFields.map(fieldName => (
          <div key={fieldName} className="criteria-field">
            <label htmlFor={fieldName}>
              {fieldLabels[fieldName]}
            </label>
            <select
              id={fieldName}
              value={formData.evaluationCriteria[fieldName] || ''}
              onChange={(e) => handleCriteriaChange(fieldName, e.target.value)}
              required
            >
              <option value="">Seleccionar...</option>
              <option value="Insufficient">I - Insuficiente</option>
              <option value="Acceptable">A - Aceptable</option>
              <option value="Good">B - Bueno</option>
              <option value="Excellent">E - Excelente</option>
            </select>
          </div>
        ))}
      </div>

      {/* Mención Propuesta */}
      <div className="form-group">
        <label htmlFor="mention">Mención Propuesta:</label>
        <select
          id="mention"
          value={formData.evaluationCriteria.proposedMention || 'NONE'}
          onChange={(e) => handleCriteriaChange('proposedMention', e.target.value)}
        >
          <option value="NONE">Ninguna</option>
          <option value="MERITORIOUS">Meritoria</option>
          <option value="LAUREATE">Laureada</option>
        </select>
      </div>

      {/* Observaciones */}
      <div className="form-group">
        <label htmlFor="observations">Observaciones (mínimo 10 caracteres):</label>
        <textarea
          id="observations"
          value={formData.observations}
          onChange={(e) => setFormData({ ...formData, observations: e.target.value })}
          minLength="10"
          maxLength="2000"
          required
          rows="4"
        />
      </div>

      <button type="submit" disabled={loading}>
        {loading ? 'Enviando...' : 'Guardar Evaluación'}
      </button>
    </form>
  );
};
```

### 3. Componente Angular para Formulario Dinámico

```typescript
// final-defense-evaluation.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ModalityService } from './modality.service';

@Component({
  selector: 'app-final-defense-evaluation',
  templateUrl: './final-defense-evaluation.component.html',
  styleUrls: ['./final-defense-evaluation.component.css']
})
export class FinalDefenseEvaluationComponent implements OnInit {
  
  evaluationForm: FormGroup;
  rubricType: 'STANDARD' | 'ENTREPRENEURSHIP' | null = null;
  requiredFields: string[] = [];
  fieldLabels: Record<string, string> = {};
  loading = false;
  error: string | null = null;
  success = false;
  
  private modalityId: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private modalityService: ModalityService
  ) {
    this.evaluationForm = this.fb.group({
      grade: [3.5, [Validators.required, Validators.min(0), Validators.max(5)]],
      observations: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
      evaluationCriteria: this.fb.group({})
    });
  }

  ngOnInit(): void {
    this.modalityId = +this.route.snapshot.paramMap.get('id');
    this.loadModalityDetails();
  }

  loadModalityDetails(): void {
    this.modalityService.getStudentModalityDetail(this.modalityId).subscribe(
      (data) => {
        // Detectar rúbrica
        this.rubricType = this.detectRubricType(data.modalityName);
        this.requiredFields = this.getRequiredFields(this.rubricType);
        this.fieldLabels = this.getFieldLabels(this.rubricType);
        
        // Crear controles dinámicos
        this.setupCriteriaFields();
      },
      (error) => {
        this.error = 'Error al cargar los detalles de la modalidad';
      }
    );
  }

  setupCriteriaFields(): void {
    const criteriaGroup = this.evaluationForm.get('evaluationCriteria') as FormGroup;
    
    this.requiredFields.forEach(field => {
      criteriaGroup.addControl(field, this.fb.control('', Validators.required));
    });
    
    // Mención propuesta
    criteriaGroup.addControl('proposedMention', this.fb.control('NONE'));
  }

  detectRubricType(modalityName: string): 'STANDARD' | 'ENTREPRENEURSHIP' {
    const normalized = this.normalizeText(modalityName);
    if (normalized.includes('emprendimiento')) {
      return 'ENTREPRENEURSHIP';
    }
    return 'STANDARD';
  }

  private normalizeText(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  getRequiredFields(rubricType: 'STANDARD' | 'ENTREPRENEURSHIP'): string[] {
    if (rubricType === 'ENTREPRENEURSHIP') {
      return [
        'entrepreneurshipPresentationSupportMaterial',
        'entrepreneurshipCoherentBusinessObjectives',
        'entrepreneurshipMethodologyTechnicalApproach',
        'entrepreneurshipAnalyticalCreativeCapacity',
        'entrepreneurshipDefenseSustentation'
      ];
    }
    
    return [
      'domainAndClarity',
      'synthesisAndCommunication',
      'argumentationAndResponse',
      'innovationAndImpact',
      'professionalPresentation'
    ];
  }

  getFieldLabels(rubricType: 'STANDARD' | 'ENTREPRENEURSHIP'): Record<string, string> {
    if (rubricType === 'ENTREPRENEURSHIP') {
      return {
        'entrepreneurshipPresentationSupportMaterial': 
          '1. Presentación y manejo del material de apoyo',
        'entrepreneurshipCoherentBusinessObjectives': 
          '2. Formulación coherente de objetivos empresariales',
        'entrepreneurshipMethodologyTechnicalApproach': 
          '3. Metodología y enfoque técnico',
        'entrepreneurshipAnalyticalCreativeCapacity': 
          '4. Capacidad analítica con enfoque creativo',
        'entrepreneurshipDefenseSustentation': 
          '5. Defensa y sustentación de la propuesta empresarial'
      };
    }
    
    return {
      'domainAndClarity': '1. Dominio del tema y claridad conceptual (30%)',
      'synthesisAndCommunication': '2. Capacidad de síntesis y comunicación oral (15%)',
      'argumentationAndResponse': '3. Argumentación y capacidad de respuesta (30%)',
      'innovationAndImpact': '4. Innovación, impacto y aplicación (15%)',
      'professionalPresentation': '5. Presentación profesional y material (10%)'
    };
  }

  onSubmit(): void {
    if (!this.evaluationForm.valid) {
      this.error = 'Por favor completa todos los campos requeridos';
      return;
    }

    this.loading = true;
    this.error = null;

    this.modalityService.registerFinalDefenseEvaluation(
      this.modalityId,
      this.evaluationForm.value
    ).subscribe(
      (response) => {
        this.success = true;
        setTimeout(() => {
          window.location.reload();
        }, 2000);
      },
      (error) => {
        this.error = error.error.message || 'Error al registrar evaluación';
        this.loading = false;
      }
    );
  }
}
```

---

## 🎨 PLANTILLA HTML

```html
<!-- final-defense-evaluation.component.html -->
<div class="evaluation-container">
  <h1>Evaluación Final de Defensa</h1>
  
  <div *ngIf="rubricType" class="rubric-info">
    <span class="badge" [ngClass]="rubricType === 'ENTREPRENEURSHIP' ? 'badge-info' : 'badge-primary'">
      {{ rubricType === 'ENTREPRENEURSHIP' ? 'Rúbrica Empresarial' : 'Rúbrica Estándar' }}
    </span>
  </div>

  <form [formGroup]="evaluationForm" (ngSubmit)="onSubmit()">
    
    <!-- Mensajes -->
    <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
    <div *ngIf="success" class="alert alert-success">¡Evaluación guardada exitosamente!</div>

    <!-- Calificación -->
    <div class="form-group">
      <label for="grade">Calificación (0.0 - 5.0):</label>
      <input
        type="number"
        id="grade"
        class="form-control"
        formControlName="grade"
        min="0"
        max="5"
        step="0.1"
        required
      />
    </div>

    <!-- Criterios -->
    <div class="rubric-section">
      <h3>Criterios de Evaluación</h3>
      <div formGroupName="evaluationCriteria">
        <div *ngFor="let field of requiredFields" class="criteria-field">
          <label [for]="field">{{ fieldLabels[field] }}</label>
          <select
            [id]="field"
            class="form-control"
            [formControlName]="field"
            required
          >
            <option value="">Seleccionar...</option>
            <option value="Insufficient">I - Insuficiente</option>
            <option value="Acceptable">A - Aceptable</option>
            <option value="Good">B - Bueno</option>
            <option value="Excellent">E - Excelente</option>
          </select>
        </div>

        <!-- Mención Propuesta -->
        <div class="criteria-field">
          <label for="mention">Mención Propuesta:</label>
          <select id="mention" class="form-control" formControlName="proposedMention">
            <option value="NONE">Ninguna</option>
            <option value="MERITORIOUS">Meritoria</option>
            <option value="LAUREATE">Laureada</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Observaciones -->
    <div class="form-group">
      <label for="observations">Observaciones:</label>
      <textarea
        id="observations"
        class="form-control"
        formControlName="observations"
        minlength="10"
        maxlength="2000"
        rows="4"
        required
      ></textarea>
      <small class="form-text text-muted">
        {{ evaluationForm.get('observations')?.value?.length || 0 }}/2000
      </small>
    </div>

    <button
      type="submit"
      class="btn btn-primary"
      [disabled]="loading || !evaluationForm.valid"
    >
      {{ loading ? 'Enviando...' : 'Guardar Evaluación' }}
    </button>
  </form>
</div>
```

---

## 🔍 PRUEBAS RECOMENDADAS EN FRONTEND

```typescript
// final-defense-evaluation.component.spec.ts
describe('FinalDefenseEvaluationComponent', () => {
  
  it('detecta correctamente rúbrica STANDARD', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    const result = component.detectRubricType('Pasantía');
    expect(result).toBe('STANDARD');
  });

  it('detecta correctamente rúbrica ENTREPRENEURSHIP', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    const result = component.detectRubricType('Emprendimiento y fortalecimiento de empresa');
    expect(result).toBe('ENTREPRENEURSHIP');
  });

  it('normaliza correctamente nombres con acentos', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    const normalized = component['normalizeText']('EMPRENDIMIENTO Y FORTALECIMIENTO');
    expect(normalized).toContain('emprendimiento');
  });

  it('obtiene campos correctos para STANDARD', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    const fields = component.getRequiredFields('STANDARD');
    expect(fields).toContain('domainAndClarity');
    expect(fields).toContain('synthesisAndCommunication');
    expect(fields.length).toBe(5);
  });

  it('obtiene campos correctos para ENTREPRENEURSHIP', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    const fields = component.getRequiredFields('ENTREPRENEURSHIP');
    expect(fields).toContain('entrepreneurshipPresentationSupportMaterial');
    expect(fields).toContain('entrepreneurshipCoherentBusinessObjectives');
    expect(fields.length).toBe(5);
  });

  it('valida que todos los criterios sean requeridos', () => {
    const component = new FinalDefenseEvaluationComponent(...);
    component.rubricType = 'STANDARD';
    component.setupCriteriaFields();
    
    const criteriaGroup = component.evaluationForm.get('evaluationCriteria');
    expect(criteriaGroup.get('domainAndClarity').hasError('required')).toBeTruthy();
  });
});
```

---

**Última actualización: 13 de Marzo de 2025**

