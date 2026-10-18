package cl.duoc.digitalfix.workorders.repository;

import cl.duoc.digitalfix.workorders.domain.OrdenDeTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenDeTrabajoRepository extends JpaRepository<OrdenDeTrabajo, Long> {
    // Buscar todas las órdenes de una empresa (para mantener el aislamiento de datos)
    List<OrdenDeTrabajo> findByEmpresaId(Long empresaId);
}