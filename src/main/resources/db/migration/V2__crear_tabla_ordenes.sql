CREATE TABLE ordenes_trabajo (
                                 id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 servicio_id NUMBER NOT NULL,
                                 empresa_id NUMBER NOT NULL,
                                 creador_id VARCHAR2(100) NOT NULL,
                                 asignado_a VARCHAR2(100),
                                 estado VARCHAR2(50) NOT NULL,
                                 comentarios VARCHAR2(500),
                                 fecha_creacion TIMESTAMP NOT NULL,
                                 fecha_actualizacion TIMESTAMP NOT NULL
);