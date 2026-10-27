package cl.duoc.digitalfix.workorders.web;

import cl.duoc.digitalfix.workorders.service.OrdenEventoPublisher;
import cl.duoc.digitalfix.workorders.web.dto.OrdenEventoDto;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comentarios en cada cambio de estado, detalle de la orden con su historial,
 * filtros del listado y visibilidad por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistorialYDetalleTest {

    private static final String EMPRESA = "8";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private OrdenEventoPublisher publisher;

    private ResultActions como(String oid, String roles, MockHttpServletRequestBuilder p) throws Exception {
        return mvc.perform(p.header("X-User-Oid", oid).header("X-Company-Id", EMPRESA).header("X-Roles", roles)
                .header("X-User-Email", oid + "@digitalfix.cl").header("X-User-Name", "Nombre+de+" + oid)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private long id(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long nuevaOrden(String cliente) throws Exception {
        return id(como(cliente, "CLIENTE", post("/api/workorders").content("{\"servicioId\": 3, \"comentarios\": \"Sin luz\"}")));
    }

    /** El tecnico entra una vez (se crea su ficha) y el supervisor le asigna la orden. */
    private void asignarAlTecnico(long orden, String tecnico) throws Exception {
        long ficha = json.readTree(como(tecnico, "TECNICO", get("/api/workorders/mias"))
                .andReturn().getResponse().getContentAsString()).get("tecnico").get("id").asLong();
        como("oid-sup", "SUPERVISOR", put("/api/workorders/" + orden + "/asignacion")
                .content("{\"tecnicoId\":" + ficha + ",\"comentario\":\"Urgente\"}")).andExpect(status().isOk());
    }

    private OrdenEventoDto ultimoEvento() {
        ArgumentCaptor<OrdenEventoDto> captor = ArgumentCaptor.forClass(OrdenEventoDto.class);
        verify(publisher, atLeastOnce()).publicarEvento(captor.capture());
        List<OrdenEventoDto> todos = captor.getAllValues();
        return todos.get(todos.size() - 1);
    }

    @Test
    void elTecnicoComentaCadaPasoYQuedaEnElHistorial() throws Exception {
        long orden = nuevaOrden("oid-cli");
        asignarAlTecnico(orden, "oid-tec");

        como("oid-tec", "TECNICO", put("/api/workorders/" + orden + "/status")
                .content("{\"status\":\"EN_DESPLAZAMIENTO\",\"comentario\":\"Salgo desde la bodega\"}"))
                .andExpect(status().isOk());
        assertThat(ultimoEvento().nota()).isEqualTo("Salgo desde la bodega");

        // Sin comentario tambien se puede: es opcional.
        como("oid-tec", "TECNICO", put("/api/workorders/" + orden + "/status")
                .content("{\"status\":\"EN_EJECUCION\"}")).andExpect(status().isOk());
        assertThat(ultimoEvento().nota()).isNull();

        como("oid-tec", "TECNICO", put("/api/workorders/" + orden + "/status")
                .content("{\"status\":\"CERRADA\",\"comentario\":\"Se cambio el diferencial\"}")).andExpect(status().isOk());

        como("oid-sup", "SUPERVISOR", get("/api/workorders/" + orden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("CERRADA"))
                .andExpect(jsonPath("$.historial.length()").value(5))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("CREADA"))
                .andExpect(jsonPath("$.historial[1].estadoNuevo").value("ASIGNADA"))
                .andExpect(jsonPath("$.historial[1].comentario").value("Asignada a Nombre de oid-tec. Urgente"))
                .andExpect(jsonPath("$.historial[2].comentario").value("Salgo desde la bodega"))
                .andExpect(jsonPath("$.historial[2].autorNombre").value("Nombre de oid-tec"))
                .andExpect(jsonPath("$.historial[3].comentario").doesNotExist())
                .andExpect(jsonPath("$.historial[4].estadoAnterior").value("EN_EJECUCION"))
                .andExpect(jsonPath("$.historial[4].comentario").value("Se cambio el diferencial"));
    }

    @Test
    void elComentarioNoPuedePasarDe500Caracteres() throws Exception {
        long orden = nuevaOrden("oid-cli");
        como("oid-sup", "SUPERVISOR", put("/api/workorders/" + orden + "/status")
                .content("{\"status\":\"CANCELADA\",\"comentario\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadaRolVeElDetalleQueLeCorresponde() throws Exception {
        long deAna = nuevaOrden("oid-ana");
        asignarAlTecnico(deAna, "oid-tec2");

        como("oid-ana", "CLIENTE", get("/api/workorders/" + deAna)).andExpect(status().isOk());
        como("oid-beto", "CLIENTE", get("/api/workorders/" + deAna)).andExpect(status().isNotFound());
        como("oid-tec2", "TECNICO", get("/api/workorders/" + deAna)).andExpect(status().isOk());
        como("oid-otro-tec", "TECNICO", get("/api/workorders/" + deAna)).andExpect(status().isNotFound());
        como("oid-aud", "AUDITOR", get("/api/workorders/" + deAna)).andExpect(status().isOk());
        mvc.perform(get("/api/workorders/" + deAna).header("X-User-Oid", "x").header("X-Company-Id", "99")
                .header("X-Roles", "ADMIN")).andExpect(status().isNotFound());
    }

    @Test
    void elClienteVeSoloSusOrdenesYLosDemasTodas() throws Exception {
        long deAna = nuevaOrden("oid-ana");
        long deBeto = nuevaOrden("oid-beto");

        como("oid-ana", "CLIENTE", get("/api/workorders"))
                .andExpect(jsonPath("$[?(@.id == " + deAna + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + deBeto + ")]").doesNotExist());
        como("oid-sup", "SUPERVISOR", get("/api/workorders"))
                .andExpect(jsonPath("$[?(@.id == " + deAna + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + deBeto + ")]").exists());
    }

    @Test
    void filtraPorEstadoYFechas() throws Exception {
        long creada = nuevaOrden("oid-cli");
        long cancelada = nuevaOrden("oid-cli");
        como("oid-sup", "SUPERVISOR", put("/api/workorders/" + cancelada + "/status")
                .content("{\"status\":\"CANCELADA\"}")).andExpect(status().isOk());
        String hoy = LocalDate.now().toString();

        como("oid-sup", "SUPERVISOR", get("/api/workorders").param("status", "CANCELADA").param("from", hoy).param("to", hoy))
                .andExpect(jsonPath("$[?(@.id == " + cancelada + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + creada + ")]").doesNotExist());

        como("oid-sup", "SUPERVISOR", get("/api/workorders").param("from", LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.length()").value(0));

        como("oid-sup", "SUPERVISOR", get("/api/workorders").param("status", "PERDIDA"))
                .andExpect(status().isBadRequest());
        como("oid-sup", "SUPERVISOR", get("/api/workorders").param("from", hoy).param("to", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elEventoSiempreLlevaCorrelacion() throws Exception {
        nuevaOrden("oid-cli");
        assertThat(ultimoEvento().correlacionId()).isNotBlank();
    }
}
