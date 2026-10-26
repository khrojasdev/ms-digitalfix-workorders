package cl.duoc.digitalfix.workorders.domain;

import org.junit.jupiter.api.Test;

import static cl.duoc.digitalfix.workorders.domain.EstadoOrden.*;
import static org.assertj.core.api.Assertions.assertThat;

class EstadoOrdenTest {

    @Test
    void sigueElCicloDelCaso() {
        assertThat(ASIGNADA.puedePasarA(EN_DESPLAZAMIENTO)).isTrue();
        assertThat(EN_DESPLAZAMIENTO.puedePasarA(EN_EJECUCION)).isTrue();
        assertThat(EN_EJECUCION.puedePasarA(CERRADA)).isTrue();
    }

    @Test
    void noSeSaltanEtapas() {
        assertThat(CREADA.puedePasarA(EN_EJECUCION)).isFalse();
        assertThat(CREADA.puedePasarA(CERRADA)).isFalse();
        assertThat(ASIGNADA.puedePasarA(CERRADA)).isFalse();
    }

    @Test
    void asignadaNoEsUnCambioDeEstadoSuelto() {
        assertThat(CREADA.puedePasarA(ASIGNADA)).isFalse();
        assertThat(CREADA.admiteAsignacion()).isTrue();
        assertThat(ASIGNADA.admiteAsignacion()).isTrue();
        assertThat(EN_DESPLAZAMIENTO.admiteAsignacion()).isFalse();
    }

    @Test
    void cerradaYCanceladaSonFinales() {
        assertThat(CERRADA.siguientes()).isEmpty();
        assertThat(CANCELADA.siguientes()).isEmpty();
        assertThat(EN_EJECUCION.puedePasarA(CANCELADA)).isFalse();
    }

    @Test
    void interpretaElTextoDelCliente() {
        assertThat(desdeTexto("EN_EJECUCIÓN")).isEqualTo(EN_EJECUCION);
        assertThat(desdeTexto("en desplazamiento")).isEqualTo(EN_DESPLAZAMIENTO);
        assertThat(desdeTexto("INICIADA")).isEqualTo(EN_EJECUCION);
        assertThat(desdeTexto("cerrada")).isEqualTo(CERRADA);
        assertThat(desdeTexto("PERDIDA")).isNull();
        assertThat(desdeTexto(" ")).isNull();
    }
}
