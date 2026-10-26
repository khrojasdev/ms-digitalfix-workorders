-- Tecnico asignado a la orden. asignado_a se mantiene con su nombre para
-- mostrarlo sin consultar la tabla de tecnicos.
ALTER TABLE ordenes_trabajo ADD tecnico_id NUMBER;

CREATE INDEX idx_ordenes_tecnico ON ordenes_trabajo (tecnico_id);
