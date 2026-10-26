-- Tecnicos de terreno de cada empresa. Son un recurso que el supervisor asigna
-- a las ordenes; no son usuarios del sistema (no inician sesion).
CREATE TABLE tecnicos (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id NUMBER NOT NULL,
    nombre VARCHAR2(120) NOT NULL,
    email VARCHAR2(150) NOT NULL,
    telefono VARCHAR2(30),
    especialidad VARCHAR2(100),
    activo NUMBER(1) DEFAULT 1 NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    CONSTRAINT ck_tecnicos_activo CHECK (activo IN (0, 1)),
    CONSTRAINT uk_tecnicos_email UNIQUE (empresa_id, email)
);

CREATE INDEX idx_tecnicos_empresa ON tecnicos (empresa_id, activo);
