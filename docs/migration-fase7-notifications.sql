-- Migración Fase 7 — Notificaciones (T7.4 columnas de entrega + T7.7 tipos muertos)
-- Proyecto: SIGMA backend. Para MySQL/MariaDB. Ejecutar en prod ANTES del deploy.
-- En dev las columnas de entrega se crean solas vía ddl-auto=update (entidad Notification).

-- 1) T7.4 — Columnas de entrega (retry de dispatch)
ALTER TABLE notification ADD COLUMN delivery_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE notification ADD COLUMN last_attempt_at DATETIME NULL;
ALTER TABLE notification ADD COLUMN attachment_path VARCHAR(1000) NULL;
ALTER TABLE notification ADD COLUMN attachment_name VARCHAR(255) NULL;

-- 2) T7.7 — Migración de tipos muertos eliminados del enum NotificationType.
--    Ejecutar ANTES del deploy: las filas con tipos inexistentes romperían la
--    lectura (@Enumerated(STRING)) al desplegar el enum reducido.
--    Mapeo de filas históricas (solo in-app, sin re-envío de correos).
UPDATE notification SET type = 'MODALITY_CANCELLATION_APPROVED' WHERE type = 'MODALITY_CANCELLED';
UPDATE notification SET type = 'CORRECTION_APPROVED' WHERE type = 'DOCUMENT_APPROVED';
UPDATE notification SET type = 'CORRECTION_REJECTED_FINAL' WHERE type = 'DOCUMENT_REJECTED';
UPDATE notification SET type = 'DIRECTOR_ASSIGNED' WHERE type = 'DIRECTOR_CHANGED';
UPDATE notification SET type = 'MODALITY_REJECTED' WHERE type = 'FINAL_FAILED';
UPDATE notification SET type = 'MODALITY_INVITATION_REJECTED' WHERE type IN ('MODALITY_INVITATION_CANCELLED','MODALITY_MEMBER_LEFT');
UPDATE notification SET type = 'MODALITY_INVITATION_ACCEPTED' WHERE type = 'MODALITY_MEMBER_JOINED';
UPDATE notification SET type = 'MODALITY_STARTED' WHERE type = 'MODALITY_GROUP_READY';