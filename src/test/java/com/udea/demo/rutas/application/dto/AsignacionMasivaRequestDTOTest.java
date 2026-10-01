package com.udea.demo.rutas.application.dto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class AsignacionMasivaRequestDTOTest {
    @Test void exigeConductorYPedidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new AsignacionMasivaRequestDTO(5L, List.of(10L,20L)))).isEmpty();
            assertThat(validator.validate(new AsignacionMasivaRequestDTO(0L, List.of()))).isNotEmpty();
        }
    }
}
