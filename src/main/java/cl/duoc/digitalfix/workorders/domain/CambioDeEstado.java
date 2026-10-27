package cl.duoc.digitalfix.workorders.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un paso en la vida de una orden: de que estado a que estado paso, quien lo
 * hizo y, si quiso, un comentario. Solo se agrega, nunca se modifica.
 */
@Entity
@Table(name = "historial_orden")
public class CambioDeEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 50, updatable = false)
    private EstadoOrden estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 50, updatable = false)
    private EstadoOrden estadoNuevo;

    @Column(name = "autor_id", length = 100, updatable = false)
    private String autorId;

    @Column(name = "autor_nombre", length = 150, updatable = false)
    private String autorNombre;

    @Column(length = 500, updatable = false)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected CambioDeEstado() {}

    public CambioDeEstado(OrdenDeTrabajo orden, EstadoOrden estadoAnterior, String autorId, String autorNombre,
                          String comentario) {
        this.ordenId = orden.getId();
        this.empresaId = orden.getEmpresaId();
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = orden.getEstado();
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.comentario = comentario;
    }

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public Long getEmpresaId() { return empresaId; }
    public EstadoOrden getEstadoAnterior() { return estadoAnterior; }
    public EstadoOrden getEstadoNuevo() { return estadoNuevo; }
    public String getAutorId() { return autorId; }
    public String getAutorNombre() { return autorNombre; }
    public String getComentario() { return comentario; }
    public LocalDateTime getFecha() { return fecha; }
}
