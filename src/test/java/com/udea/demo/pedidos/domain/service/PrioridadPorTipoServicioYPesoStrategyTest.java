package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.model.Prioridad;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de la estrategia de sugerencia de prioridad.
 *
 * Cada prueba sigue el patrón AAA (Arrange - Act - Assert) con sus tres secciones marcadas.
 *
 * CP relacionados:
 *  - CP-HU03A-03: sugerencia de prioridad basada en tipo de servicio y peso.
 */
@DisplayName("PrioridadPorTipoServicioYPesoStrategy - sugerencia de prioridad")
class PrioridadPorTipoServicioYPesoStrategyTest {

    private final PrioridadPorTipoServicioYPesoStrategy estrategia = new PrioridadPorTipoServicioYPesoStrategy();

    @Test
    @DisplayName("EXPRESS siempre sugiere URGENTE sin importar el peso")
    void express_sugiereUrgente() {
        // Arrange
        TipoServicio servicio = TipoServicio.EXPRESS;
        Double peso = 1.0;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.URGENTE);
    }

    @Test
    @DisplayName("peso mayor a 20 kg sugiere ALTA (servicio no express)")
    void pesoMayorAlUmbral_sugiereAlta() {
        // Arrange
        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 25.0;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("peso igual al umbral (20 kg) no se considera ALTA")
    void pesoIgualAlUmbral_noEsAlta() {
        // Arrange
        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 20.0;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("PROGRAMADO (peso normal) sugiere BAJA")
    void programado_sugiereBaja() {
        // Arrange
        TipoServicio servicio = TipoServicio.PROGRAMADO;
        Double peso = 5.0;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.BAJA);
    }

    @Test
    @DisplayName("ESTANDAR con peso normal sugiere MEDIA")
    void estandar_pesoNormal_sugiereMedia() {
        // Arrange
        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = 5.0;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("peso nulo no dispara la regla de ALTA")
    void pesoNulo_noEsAlta() {
        // Arrange
        TipoServicio servicio = TipoServicio.ESTANDAR;
        Double peso = null;

        // Act
        Prioridad resultado = estrategia.sugerir(servicio, peso);

        // Assert
        assertThat(resultado).isEqualTo(Prioridad.MEDIA);
    }
}
