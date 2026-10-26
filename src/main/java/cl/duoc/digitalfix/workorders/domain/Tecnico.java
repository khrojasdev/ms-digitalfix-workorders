package cl.duoc.digitalfix.workorders.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tecnico de terreno de una empresa. Es un recurso que se asigna a las ordenes
 * (no un usuario con sesion): recibe el ticket de despacho por correo.
 */
@Entity
@Table(name = "tecnicos")
public class Tecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String especialidad;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected Tecnico() {}

    public Tecnico(Long empresaId, String nombre, String email, String telefono, String especialidad) {
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.especialidad = especialidad;
        this.activo = true;
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public void actualizar(String nombre, String email, String telefono, String especialidad) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.especialidad = especialidad;
    }

    /** Desactivacion logica: deja de ofrecerse para asignar, pero conserva su historial. */
    public void desactivar() { this.activo = false; }
    public void reactivar() { this.activo = true; }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getEspecialidad() { return especialidad; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
