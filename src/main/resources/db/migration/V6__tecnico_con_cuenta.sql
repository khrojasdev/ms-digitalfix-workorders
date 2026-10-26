-- Rol TECNICO: el tecnico inicia sesion y ve sus ordenes. La ficha se vincula
-- con su cuenta de Entra ID (oid) la primera vez que entra, por el correo.
ALTER TABLE tecnicos ADD usuario_oid VARCHAR2(100);

-- Una cuenta corresponde a una sola ficha. Oracle no indexa los NULL, asi que
-- las fichas aun sin vincular no chocan entre si.
CREATE UNIQUE INDEX uk_tecnicos_usuario ON tecnicos (usuario_oid);
