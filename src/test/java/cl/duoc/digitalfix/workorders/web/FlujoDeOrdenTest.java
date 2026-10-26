package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.service.OrdenEventoPublisher;
import cl.duoc.digitalfix.workorders.web.dto.OrdenEventoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre el flujo del caso contra la API real (H2 en memoria): alta de
 * tecnico, orden, asignacion y cambios de estado, con las cabeceras que pone el BFF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FlujoDeOrdenTest {

    private static final String EMPRESA = "7";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private OrdenEventoPublisher publisher;

    private MockHttpServletRequestBuilder conContexto(MockHttpServletRequestBuilder peticion, String empresa, String roles) {
        return peticion.header("X-User-Oid", "oid-supervisor")
                .header("X-Company-Id", empresa)
                .header("X-Roles", roles)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private ResultActions como(String roles, MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(conContexto(peticion, EMPRESA, roles));
    }

    private long crearTecnico(String email) throws Exception {
        String cuerpo = como("SUPERVISOR", post("/api/workorders/tecnicos")
                .content("{\"nombre\":\"Pedro Soto\",\"email\":\"" + email + "\",\"telefono\":\"+56911112222\",\"especialidad\":\"Redes\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }

    private long crearOrden() throws Exception {
        String cuerpo = como("CLIENTE", post("/api/workorders")
                .content("{\"servicioId\": 3, \"comentarios\": \"No enciende el router\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }

    private ResultActions cambiarA(long orden, String estado) throws Exception {
        return como("SUPERVISOR", put("/api/workorders/" + orden + "/status").content("{\"status\":\"" + estado + "\"}"));
    }

    private ResultActions asignar(long orden, long tecnico) throws Exception {
        return como("SUPERVISOR", put("/api/workorders/" + orden + "/asignacion").content("{\"tecnicoId\":" + tecnico + "}"));
    }

    private OrdenEventoDto ultimoEvento() {
        ArgumentCaptor<OrdenEventoDto> captor = ArgumentCaptor.forClass(OrdenEventoDto.class);
        verify(publisher, atLeastOnce()).publicarEvento(captor.capture());
        List<OrdenEventoDto> todos = captor.getAllValues();
        return todos.get(todos.size() - 1);
    }

    @Test
    void recorreElCicloCompletoDelCaso() throws Exception {
        long tecnico = crearTecnico("pedro@digitalfix.cl");
        long orden = crearOrden();

        asignar(orden, tecnico)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.asignadoA").value("Pedro Soto"))
                .andExpect(jsonPath("$.tecnicoId").value(tecnico));

        OrdenEventoDto asignada = ultimoEvento();
        assertThat(asignada.tipoEvento()).isEqualTo("ASIGNADA");
        assertThat(asignada.estadoAnterior()).isEqualTo("CREADA");
        assertThat(asignada.servicioId()).isEqualTo(3L);
        assertThat(asignada.tecnicoEmail()).isEqualTo("pedro@digitalfix.cl");
        assertThat(asignada.creadorId()).isEqualTo("oid-supervisor");

        cambiarA(orden, "EN_DESPLAZAMIENTO").andExpect(status().isOk());
        assertThat(ultimoEvento().tecnicoNombre()).isEqualTo("Pedro Soto");
        cambiarA(orden, "EN_EJECUCIÓN").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_EJECUCION"));
        cambiarA(orden, "CERRADA").andExpect(status().isOk());

        OrdenEventoDto cerrada = ultimoEvento();
        assertThat(cerrada.tipoEvento()).isEqualTo("CERRADA");
        assertThat(cerrada.estadoAnterior()).isEqualTo("EN_EJECUCION");

        cambiarA(orden, "CANCELADA").andExpect(status().isConflict());
    }

    @Test
    void noPasaAEnEjecucionSinAsignar() throws Exception {
        long orden = crearOrden();

        cambiarA(orden, "EN_EJECUCION")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("No se puede pasar a EN_EJECUCION sin ASIGNAR un tecnico."));
    }

    @Test
    void asignadaSoloSeLograAsignandoTecnico() throws Exception {
        long orden = crearOrden();
        cambiarA(orden, "ASIGNADA").andExpect(status().isConflict());
    }

    @Test
    void elEndpointAnteriorSigueFuncionando() throws Exception {
        long orden = crearOrden();
        como("SUPERVISOR", put("/api/workorders/" + orden + "/estado").param("nuevoEstado", "CANCELADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void estadoDesconocidoEs400() throws Exception {
        long orden = crearOrden();
        cambiarA(orden, "PERDIDA").andExpect(status().isBadRequest());
    }

    @Test
    void reasignarAntesDeSalirATerreno() throws Exception {
        long primero = crearTecnico("uno@digitalfix.cl");
        long segundo = crearTecnico("dos@digitalfix.cl");
        long orden = crearOrden();

        asignar(orden, primero).andExpect(status().isOk());
        asignar(orden, segundo).andExpect(status().isOk());

        OrdenEventoDto reasignada = ultimoEvento();
        assertThat(reasignada.estadoAnterior()).isEqualTo("ASIGNADA");
        assertThat(reasignada.tecnicoEmail()).isEqualTo("dos@digitalfix.cl");

        cambiarA(orden, "EN_DESPLAZAMIENTO").andExpect(status().isOk());
        asignar(orden, primero).andExpect(status().isConflict());
    }

    @Test
    void noSeAsignaUnTecnicoDesactivado() throws Exception {
        long tecnico = crearTecnico("baja@digitalfix.cl");
        como("ADMIN", delete("/api/workorders/tecnicos/" + tecnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        asignar(crearOrden(), tecnico).andExpect(status().isConflict());

        String activos = como("SUPERVISOR", get("/api/workorders/tecnicos").param("soloActivos", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode lista = json.readTree(activos);
        assertThat(lista.findValuesAsText("email")).doesNotContain("baja@digitalfix.cl");
    }

    @Test
    void elClienteNoGestionaTecnicosNiAsigna() throws Exception {
        long tecnico = crearTecnico("cliente@digitalfix.cl");
        long orden = crearOrden();

        como("CLIENTE", post("/api/workorders/tecnicos")
                .content("{\"nombre\":\"X\",\"email\":\"x@digitalfix.cl\"}"))
                .andExpect(status().isForbidden());
        como("CLIENTE", put("/api/workorders/" + orden + "/asignacion").content("{\"tecnicoId\":" + tecnico + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadaEmpresaVeSoloSusTecnicos() throws Exception {
        long tecnico = crearTecnico("propio@digitalfix.cl");

        mvc.perform(conContexto(put("/api/workorders/tecnicos/" + tecnico)
                        .content("{\"nombre\":\"Otro\",\"email\":\"otro@digitalfix.cl\"}"), "99", "ADMIN"))
                .andExpect(status().isNotFound());

        long ordenDeOtra = json.readTree(mvc.perform(conContexto(post("/api/workorders")
                        .content("{\"servicioId\": 1}"), "99", "CLIENTE"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        asignar(ordenDeOtra, tecnico).andExpect(status().isNotFound());
    }

    @Test
    void correoRepetidoEs409() throws Exception {
        crearTecnico("repetido@digitalfix.cl");
        como("SUPERVISOR", post("/api/workorders/tecnicos")
                .content("{\"nombre\":\"Otra\",\"email\":\"REPETIDO@digitalfix.cl\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test
    void validaLosDatosDelTecnico() throws Exception {
        como("SUPERVISOR", post("/api/workorders/tecnicos").content("{\"nombre\":\"\",\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
    }
}
