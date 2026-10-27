-- Historial de cada orden: cada cambio de estado (y la creacion y la
-- asignacion) con quien lo hizo y un comentario opcional. Es lo que muestra
-- el detalle de la orden; el tecnico lo usa para dejar notas del trabajo.
CREATE TABLE historial_orden (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    orden_id NUMBER NOT NULL,
    empresa_id NUMBER NOT NULL,
    estado_anterior VARCHAR2(50),
    estado_nuevo VARCHAR2(50) NOT NULL,
    autor_id VARCHAR2(100),
    autor_nombre VARCHAR2(150),
    comentario VARCHAR2(500),
    fecha TIMESTAMP NOT NULL,
    CONSTRAINT fk_historial_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo (id)
);

CREATE INDEX idx_historial_orden ON historial_orden (orden_id, fecha);
