-- El estado INICIADA pasa a llamarse EN_EJECUCION, como en el caso.
-- Se agrega EN_DESPLAZAMIENTO (no requiere cambio de esquema: estado es VARCHAR2).
UPDATE ordenes_trabajo SET estado = 'EN_EJECUCION' WHERE estado = 'INICIADA';
