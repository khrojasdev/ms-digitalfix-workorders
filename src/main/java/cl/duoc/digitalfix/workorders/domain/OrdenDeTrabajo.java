package cl.duoc.digitalfix.workorders.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_trabajo")
public class OrdenDeTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "creador_id", nullable = false)
    private String creadorId;

    @Column(name = "asignado_a")
    private String asignadoA;

    @Column(name = "tecnico_id")
    private Long tecnicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    @Column(length = 500)
    private String comentarios;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public OrdenDeTrabajo() {}

    public OrdenDeTrabajo(Long servicioId, Long empresaId, String creadorId, String comentarios) {
        this.servicioId = servicioId;
        this.empresaId = empresaId;
        this.creadorId = creadorId;
        this.comentarios = comentarios;
        this.estado = EstadoOrden.CREADA;
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getServicioId() { return servicioId; }
    public void setServicioId(Long servicioId) { this.servicioId = servicioId; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getCreadorId() { return creadorId; }
    public void setCreadorId(String creadorId) { this.creadorId = creadorId; }
    public String getAsignadoA() { return asignadoA; }
    public void setAsignadoA(String asignadoA) { this.asignadoA = asignadoA; }
    public Long getTecnicoId() { return tecnicoId; }

    /** Deja la orden ASIGNADA al tecnico; guarda tambien su nombre para mostrarlo. */
    public void asignarA(Tecnico tecnico) {
        this.tecnicoId = tecnico.getId();
        this.asignadoA = tecnico.getNombre();
        this.estado = EstadoOrden.ASIGNADA;
    }

    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}