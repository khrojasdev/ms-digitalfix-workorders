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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rol TECNICO: entra con su cuenta, queda vinculado a su ficha por el correo,
 * ve solo sus ordenes y las avanza en terreno.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RolTecnicoTest {

    private static final String EMPRESA = "7";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private OrdenEventoPublisher publisher;

    private ResultActions supervisor(MockHttpServletRequestBuilder p) throws Exception {
        return mvc.perform(p.header("X-User-Oid", "oid-sup").header("X-Company-Id", EMPRESA)
                .header("X-Roles", "SUPERVISOR").contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions tecnico(String oid, String email, MockHttpServletRequestBuilder p) throws Exception {
        return mvc.perform(p.header("X-User-Oid", oid).header("X-Company-Id", EMPRESA)
                .header("X-Roles", "TECNICO").header("X-User-Email", email).contentType(MediaType.APPLICATION_JSON));
    }

    private long id(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long altaTecnico(String email) throws Exception {
        return id(supervisor(post("/api/workorders/tecnicos")
                .content("{\"nombre\":\"Pedro Soto\",\"email\":\"" + email + "\"}")).andExpect(status().isCreated()));
    }

    private long ordenAsignadaA(long tecnico) throws Exception {
        long orden = id(supervisor(post("/api/workorders").content("{\"servicioId\": 3, \"comentarios\": \"Calle 1\"}")));
        supervisor(put("/api/workorders/" + orden + "/asignacion").content("{\"tecnicoId\":" + tecnico + "}"))
                .andExpect(status().isOk());
        return orden;
    }

    private ResultActions marcar(String oid, String email, long orden, String estado) throws Exception {
        return tecnico(oid, email, put("/api/workorders/" + orden + "/status").content("{\"status\":\"" + estado + "\"}"));
    }

    @Test
    void entraPorPrimeraVezYQuedaVinculadoPorElCorreo() throws Exception {
        long ficha = altaTecnico("pedro@digitalfix.cl");
        long orden = ordenAsignadaA(ficha);

        tecnico("oid-pedro", "Pedro@DigitalFix.cl", get("/api/workorders/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnico.id").value(ficha))
                .andExpect(jsonPath("$.tecnico.cuentaVinculada").value(true))
                .andExpect(jsonPath("$.ordenes.length()").value(1))
                .andExpect(jsonPath("$.ordenes[0].id").value(orden));

        // Ya vinculado, lo reconoce por su cuenta aunque no llegue el correo.
        tecnico("oid-pedro", "", get("/api/workorders/mias")).andExpect(status().isOk());
    }

    @Test
    void avanzaSuOrdenEnTerrenoHastaCerrarla() throws Exception {
        long orden = ordenAsignadaA(altaTecnico("ana@digitalfix.cl"));

        marcar("oid-ana", "ana@digitalfix.cl", orden, "EN_DESPLAZAMIENTO").andExpect(status().isOk());
        marcar("oid-ana", "ana@digitalfix.cl", orden, "EN_EJECUCION").andExpect(status().isOk());
        marcar("oid-ana", "ana@digitalfix.cl", orden, "CERRADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        ArgumentCaptor<OrdenEventoDto> eventos = ArgumentCaptor.forClass(OrdenEventoDto.class);
        verify(publisher, atLeastOnce()).publicarEvento(eventos.capture());
        List<OrdenEventoDto> todos = eventos.getAllValues();
        OrdenEventoDto cierre = todos.get(todos.size() - 1);
        assertThat(cierre.tipoEvento()).isEqualTo("CERRADA");
        assertThat(cierre.autorId()).isEqualTo("oid-ana");
    }

    @Test
    void noPuedeCancelarNiSaltarseEtapas() throws Exception {
        long orden = ordenAsignadaA(altaTecnico("luis@digitalfix.cl"));

        marcar("oid-luis", "luis@digitalfix.cl", orden, "CANCELADA").andExpect(status().isForbidden());
        marcar("oid-luis", "luis@digitalfix.cl", orden, "CERRADA").andExpect(status().isConflict());
    }

    @Test
    void noTocaOrdenesDeOtroTecnico() throws Exception {
        long deOtro = ordenAsignadaA(altaTecnico("otro@digitalfix.cl"));
        altaTecnico("yo@digitalfix.cl");

        marcar("oid-yo", "yo@digitalfix.cl", deOtro, "EN_DESPLAZAMIENTO")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("La orden #" + deOtro + " no esta asignada a ti."));
        tecnico("oid-yo", "yo@digitalfix.cl", get("/api/workorders/mias"))
                .andExpect(jsonPath("$.ordenes.length()").value(0));
    }

    @Test
    void sinFichaConSuCorreoRecibe404ConLaExplicacion() throws Exception {
        tecnico("oid-nuevo", "nuevo@digitalfix.cl", get("/api/workorders/mias"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(
                        "Tu cuenta (nuevo@digitalfix.cl) no esta registrada como tecnico en tu empresa. "
                                + "Pide al supervisor que te agregue con ese correo."));
    }

    @Test
    void unaFichaYaVinculadaNoSeLaQuedaOtraCuenta() throws Exception {
        altaTecnico("compartido@digitalfix.cl");
        tecnico("oid-a", "compartido@digitalfix.cl", get("/api/workorders/mias")).andExpect(status().isOk());
        tecnico("oid-b", "compartido@digitalfix.cl", get("/api/workorders/mias")).andExpect(status().isConflict());
    }

    @Test
    void conLaFichaDesactivadaNoEntra() throws Exception {
        long ficha = altaTecnico("baja@digitalfix.cl");
        supervisor(delete("/api/workorders/tecnicos/" + ficha)).andExpect(status().isOk());
        tecnico("oid-baja", "baja@digitalfix.cl", get("/api/workorders/mias")).andExpect(status().isForbidden());
    }

    @Test
    void elClienteYElAuditorNoCambianEstados() throws Exception {
        long orden = ordenAsignadaA(altaTecnico("x@digitalfix.cl"));
        for (String rol : new String[]{"CLIENTE", "AUDITOR"}) {
            mvc.perform(put("/api/workorders/" + orden + "/status").content("{\"status\":\"EN_DESPLAZAMIENTO\"}")
                            .header("X-User-Oid", "oid").header("X-Company-Id", EMPRESA).header("X-Roles", rol)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void soloElTecnicoUsaMisOrdenes() throws Exception {
        supervisor(get("/api/workorders/mias")).andExpect(status().isForbidden());
    }
}
