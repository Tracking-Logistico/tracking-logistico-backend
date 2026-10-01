package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrioridadPorTipoServicioYPesoStrategy - sugerencia de prioridad")
class PrioridadPorTipoServicioYPesoStrategyTest {

    private final PrioridadPorTipoServicioYPesoStrategy estrategia = new PrioridadPorTipoServicioYPesoStrategy();

    @Test
    @DisplayName("EXPRESS siempre sugiere ALTA sin importar el peso")
    void express_sugiereUrgente() {

        TipoServicio servicio = TipoServicio.EXPRESS;
        Double peso = 1.0;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("El servicio ESTANDAR sugiere MEDIA independientemente del peso")
    void pesoMayorAlUmbral_sugiereAlta() {

        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 25.0;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("peso igual al umbral (20 kg) no se considera ALTA")
    void pesoIgualAlUmbral_noEsAlta() {

        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 20.0;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("PROGRAMADO (peso normal) sugiere BAJA")
    void programado_sugiereBaja() {

        TipoServicio servicio = TipoServicio.PROGRAMADO;
        Double peso = 5.0;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.BAJA);
    }

    @Test
    @DisplayName("ESTANDAR con peso normal sugiere MEDIA")
    void estandar_pesoNormal_sugiereMedia() {

        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 5.0;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("peso nulo no dispara la regla de ALTA")
    void pesoNulo_noEsAlta() {

        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = null;

        Prioridad resultado = estrategia.sugerir(servicio, peso);

        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }
}
