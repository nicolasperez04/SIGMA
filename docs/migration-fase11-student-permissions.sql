-- Migración Fase 11 — Permisos informativos para ROLE_STUDENT
-- Autorización real por ROLE_STUDENT; los PERM_* son declarativos (no se usan en @PreAuthorize).
-- Alinea la BD poblada (prod) con el seeder dev (DataInitializer). Idempotente.

INSERT INTO permissions (name)
SELECT t.name
FROM (
    SELECT 'START_MODALITY' AS name
    UNION ALL SELECT 'UPLOAD_DOCUMENT'
    UNION ALL SELECT 'REQUEST_CANCELLATION'
    UNION ALL SELECT 'REQUEST_EDIT'
    UNION ALL SELECT 'VIEW_RESULT'
) t
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = t.name);

INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('START_MODALITY','UPLOAD_DOCUMENT','REQUEST_CANCELLATION','REQUEST_EDIT','VIEW_RESULT')
WHERE r.name = 'STUDENT'
AND NOT EXISTS (SELECT 1 FROM roles_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);