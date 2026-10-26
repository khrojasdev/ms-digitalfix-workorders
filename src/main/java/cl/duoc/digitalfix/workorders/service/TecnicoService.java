package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.config.ContextoEmpresa;
import cl.duoc.digitalfix.workorders.config.Permisos;
import cl.duoc.digitalfix.workorders.domain.Tecnico;
import cl.duoc.digitalfix.workorders.error.AccesoDenegado;
import cl.duoc.digitalfix.workorders.error.Conflicto;
import cl.duoc.digitalfix.workorders.error.RecursoNoEncontrado;
import cl.duoc.digitalfix.workorders.error.SolicitudInvalida;
import cl.duoc.digitalfix.workorders.repository.TecnicoRepository;
import cl.duoc.digitalfix.workorders.web.dto.TecnicoRespuestaDto;
import cl.duoc.digitalfix.workorders.web.dto.TecnicoSolicitudDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Tecnicos de terreno de la empresa. Los gestiona el ADMIN o el SUPERVISOR. */
@Service
public class TecnicoService {

    private final TecnicoRepository repository;

    public TecnicoService(TecnicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TecnicoRespuestaDto> listar(boolean soloActivos, ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.GESTION);
        List<Tecnico> tecnicos = soloActivos
                ? repository.findByEmpresaIdAndActivoTrueOrderByNombreAsc(contexto.companyId())
                : repository.findByEmpresaIdOrderByNombreAsc(contexto.companyId());
        return tecnicos.stream().map(TecnicoRespuestaDto::desde).toList();
    }

    @Transactional
    public TecnicoRespuestaDto crear(TecnicoSolicitudDto dto, ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.GESTION);
        String email = normalizarEmail(dto.email());
        if (repository.existsByEmpresaIdAndEmailIgnoreCase(contexto.companyId(), email)) {
            throw new Conflicto("Ya existe un tecnico con el correo " + email + ".");
        }
        Tecnico tecnico = new Tecnico(contexto.companyId(), dto.nombre().trim(), email,
                limpiar(dto.telefono()), limpiar(dto.especialidad()));
        return TecnicoRespuestaDto.desde(repository.save(tecnico));
    }

    @Transactional
    public TecnicoRespuestaDto actualizar(Long id, TecnicoSolicitudDto dto, ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.GESTION);
        Tecnico tecnico = buscarDeLaEmpresa(id, contexto);
        String email = normalizarEmail(dto.email());
        if (repository.existsByEmpresaIdAndEmailIgnoreCaseAndIdNot(contexto.companyId(), email, id)) {
            throw new Conflicto("Ya existe otro tecnico con el correo " + email + ".");
        }
        tecnico.actualizar(dto.nombre().trim(), email, limpiar(dto.telefono()), limpiar(dto.especialidad()));
        if (Boolean.TRUE.equals(dto.activo())) {
            tecnico.reactivar();
        } else if (Boolean.FALSE.equals(dto.activo())) {
            tecnico.desactivar();
        }
        return TecnicoRespuestaDto.desde(repository.save(tecnico));
    }

    /** Baja logica: no se borra para no perder a quien atendio ordenes pasadas. */
    @Transactional
    public TecnicoRespuestaDto desactivar(Long id, ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.GESTION);
        Tecnico tecnico = buscarDeLaEmpresa(id, contexto);
        tecnico.desactivar();
        return TecnicoRespuestaDto.desde(repository.save(tecnico));
    }

    /** Tecnico de la empresa del contexto. Si es de otra empresa, responde como si no existiera. */
    @Transactional(readOnly = true)
    public Tecnico buscarDeLaEmpresa(Long id, ContextoEmpresa contexto) {
        if (id == null) {
            throw new SolicitudInvalida("Falta el id del tecnico.");
        }
        return repository.findByIdAndEmpresaId(id, contexto.companyId())
                .orElseThrow(() -> new RecursoNoEncontrado("Tecnico no encontrado"));
    }

    /**
     * La ficha del TECNICO que esta llamando. Se busca por su cuenta (oid); si
     * aun no esta vinculada, se busca por el correo con que inicio sesion y se
     * vincula. Asi el supervisor solo tiene que darlo de alta con el mismo
     * correo de su cuenta de Entra ID.
     */
    @Transactional
    public Tecnico delUsuario(ContextoEmpresa contexto) {
        Permisos.exigirAlguno(contexto, Permisos.TECNICO);
        if (contexto.userOid() == null || contexto.userOid().isBlank()) {
            throw new AccesoDenegado("No se pudo identificar tu cuenta.");
        }

        Optional<Tecnico> vinculado = repository.findByEmpresaIdAndUsuarioOid(contexto.companyId(), contexto.userOid());
        Tecnico tecnico = vinculado.orElseGet(() -> vincularPorCorreo(contexto));

        if (!tecnico.isActivo()) {
            throw new AccesoDenegado("Tu ficha de tecnico esta desactivada. Habla con tu supervisor.");
        }
        return tecnico;
    }

    private Tecnico vincularPorCorreo(ContextoEmpresa contexto) {
        String email = normalizarEmail(contexto.email());
        Tecnico tecnico = email == null ? null
                : repository.findByEmpresaIdAndEmailIgnoreCase(contexto.companyId(), email).orElse(null);
        if (tecnico == null) {
            throw new RecursoNoEncontrado("Tu cuenta" + (email == null ? "" : " (" + email + ")")
                    + " no esta registrada como tecnico en tu empresa. Pide al supervisor que te agregue con ese correo.");
        }
        if (tecnico.getUsuarioOid() != null) {
            throw new Conflicto("La ficha de " + tecnico.getNombre() + " ya esta vinculada a otra cuenta.");
        }
        tecnico.vincularCuenta(contexto.userOid());
        return repository.save(tecnico);
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
