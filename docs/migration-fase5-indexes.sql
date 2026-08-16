-- Migración Fase 5 — Índices compuestos para queries batch (T5.15)
-- Proyecto: SIGMA backend. Ejecutar manualmente en BD prod (prod usa ddl-auto=validate, no crea índices).
-- Los mismos índices se crean automáticamente en dev vía ddl-auto=update (@Index en las entidades).
-- Verificado 8/2026: nombres de columna = naming físico Hibernate (snake_case).

CREATE INDEX idx_member_modality_status ON student_modality_members (student_modality_id, status);
CREATE INDEX idx_member_student ON student_modality_members (student_id);
CREATE INDEX idx_defense_examiner_modality ON defense_examiners (student_modality_id);
CREATE INDEX idx_criteria_examiner ON defense_evaluation_criteria (defense_examiner_id);
CREATE INDEX idx_student_doc_modality_config ON student_documents (student_modality_id, document_config_id);
CREATE INDEX idx_notification_recipient_created ON notification (recipient_user_id, created_at);

-- T5.16 — VERIFICACIÓN PENDIENTE en prod (requiere acceso a la BD):
--   black_listed_token.token es @Column(length=5000, unique=true).
--   Con utf8mb4, un UNIQUE de 5000 chars excede el límite de 3072 bytes → MySQL
--   habría rechazado crear el índice (ERROR 1071). Comprobar:
--     SHOW INDEX FROM black_listed_token WHERE Key_name = 'token';
--   Si el índice NO existe: reducir la columna (ej. hash SHA-256 del token, 64 chars)
--   requiere migración de datos + cambio en JwtService/BlackListedTokenCleanupScheduler.
--   Mientras tanto el endpoint de logout NO está protegido por el UNIQUE (dato en sí).