-- Migración: drift de esquema detectado al arrancar con ddl-auto=validate en prod.
-- Aplicada manualmente en BD SIGMABD (2026-08-14).

-- black_listed_token: columna requerida por
-- BlackListedTokenCleanupScheduler.deleteByExpiresAtBefore. La tabla estaba sin datos;
-- NOT NULL sin default es seguro.
ALTER TABLE black_listed_token ADD COLUMN expires_at datetime(6) NOT NULL;

-- notification: columnas de la outbox de F7 (NotificationRetryScheduler).
-- Tabla con 13.840 filas => delivery_attempts con DEFAULT 0 (nullable=false).
ALTER TABLE notification ADD COLUMN delivery_attempts int NOT NULL DEFAULT 0;
ALTER TABLE notification ADD COLUMN last_attempt_at datetime(6) NULL;
ALTER TABLE notification ADD COLUMN attachment_path varchar(1000) NULL;
ALTER TABLE notification ADD COLUMN attachment_name varchar(1000) NULL;