package cl.duoc.digitalfix.workorders.repository;

import cl.duoc.digitalfix.workorders.domain.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    List<Tecnico> findByEmpresaIdAndActivoTrueOrderByNombreAsc(Long empresaId);

    Optional<Tecnico> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndEmailIgnoreCase(Long empresaId, String email);

    boolean existsByEmpresaIdAndEmailIgnoreCaseAndIdNot(Long empresaId, String email, Long id);
}
