package com.udea.demo.usuarios.application.service;

import com.udea.demo.usuarios.domain.exception.PerfilRolIncompletoException;
import com.udea.demo.usuarios.domain.model.Operador;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.domain.model.Usuario;
import com.udea.demo.usuarios.interfaces.persistence.ClienteRepository;
import com.udea.demo.usuarios.interfaces.persistence.ConductorRepository;
import com.udea.demo.usuarios.interfaces.persistence.OperadorRepository;
import com.udea.demo.usuarios.interfaces.persistence.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorAuthorizationServiceTest {
    @Mock private UsuarioRepository usuarios;
    @Mock private OperadorRepository operadores;
    @Mock private ClienteRepository clientes;
    @Mock private ConductorRepository conductores;
    @InjectMocks private ActorAuthorizationService actores;

    @AfterEach void limpiarContexto() { SecurityContextHolder.clearContext(); }

    private void autenticar(Rol rol) {
        var usuario = Usuario.builder().id(22L).email("actor@test.local")
                .rol(rol).activo(true).build();
        when(usuarios.findByEmail("actor@test.local")).thenReturn(Optional.of(usuario));
        var auth = new UsernamePasswordAuthenticationToken("actor@test.local", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test void operadorSinPerfilNoSeDisfrazaDePermisoDenegado() {
        autenticar(Rol.OPERADOR);
        when(operadores.findByUsuarioId(22L)).thenReturn(Optional.empty());
        assertThatThrownBy(actores::operadorActualId)
                .isInstanceOf(PerfilRolIncompletoException.class)
                .hasMessageContaining("OPERADOR");
    }

    @Test void operadorConPerfilEntregaIdOperadorRealParaAuditarPedido() {
        autenticar(Rol.OPERADOR);
        when(operadores.findByUsuarioId(22L)).thenReturn(Optional.of(Operador.builder().id(31L).build()));
        assertThat(actores.operadorActualId()).isEqualTo(31L);
    }

    @Test void notificacionesDeConductorSoloNecesitanElIdUsuario() {
        autenticar(Rol.CONDUCTOR);
        assertThat(actores.conductorActualUsuarioId()).isEqualTo(22L);
        verify(conductores, never()).findByUsuarioId(22L);
    }

    @Test void operadorNoPuedePedirLasNotificacionesDeConductor() {
        autenticar(Rol.OPERADOR);
        assertThatThrownBy(actores::conductorActualUsuarioId)
                .isInstanceOf(AccessDeniedException.class);
    }
}
