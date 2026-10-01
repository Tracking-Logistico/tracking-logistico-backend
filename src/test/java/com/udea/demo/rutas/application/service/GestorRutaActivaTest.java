package com.udea.demo.rutas.application.service;

import com.udea.demo.rutas.domain.model.Ruta;
import com.udea.demo.rutas.interfaces.persistence.RutaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestorRutaActiva - componente de dominio (HU-09)")
class GestorRutaActivaTest {

    @Mock
    private RutaRepository rutaRepository;

    @InjectMocks
    private GestorRutaActiva gestorRutaActiva;

    private static final Long CONDUCTOR_ID = 1L;

    @Test
    @DisplayName("obtenerOCrear() retorna la ruta existente si el conductor ya tiene ruta para hoy")
    void obtenerOCrear_rutaExistente_retornaRutaSinCrearNueva() {

        LocalDate hoy = LocalDate.now();
        Ruta rutaExistente = Ruta.crear(CONDUCTOR_ID, hoy);
        when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_ID, hoy))
                .thenReturn(Optional.of(rutaExistente));

        Ruta resultado = gestorRutaActiva.obtenerOCrear(CONDUCTOR_ID);

        assertThat(resultado).isSameAs(rutaExistente);
        assertThat(resultado.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(resultado.getFecha()).isEqualTo(hoy);
        verify(rutaRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenerOCrear() crea y persiste una nueva ruta si el conductor no tiene ruta para hoy")
    void obtenerOCrear_noExisteRuta_creaYGuardaNuevaRuta() {

        LocalDate hoy = LocalDate.now();
        when(rutaRepository.findByConductorIdAndFecha(CONDUCTOR_ID, hoy))
                .thenReturn(Optional.empty());
        when(rutaRepository.save(any(Ruta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ruta resultado = gestorRutaActiva.obtenerOCrear(CONDUCTOR_ID);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getConductorId()).isEqualTo(CONDUCTOR_ID);
        assertThat(resultado.getFecha()).isEqualTo(hoy);
        verify(rutaRepository).save(any(Ruta.class));
    }
}
