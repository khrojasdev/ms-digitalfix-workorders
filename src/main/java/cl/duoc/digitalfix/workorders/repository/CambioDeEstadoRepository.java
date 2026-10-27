package cl.duoc.digitalfix.workorders.repository;

import cl.duoc.digitalfix.workorders.domain.CambioDeEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioDeEstadoRepository extends JpaRepository<CambioDeEstado, Long> {

    List<CambioDeEstado> findByEmpresaIdAndOrdenIdOrderByFechaAscIdAsc(Long empresaId, Long ordenId);
}
