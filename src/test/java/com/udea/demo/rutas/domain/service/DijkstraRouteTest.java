package com.udea.demo.rutas.domain.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DijkstraRouteTest {
    @Test void ordenaParadasUbicadas() {
        assertEquals(List.of(1L, 2L, 3L), DijkstraRoute.order(List.of(
                new DijkstraRoute.Stop(1L, 4.60, -74.08),
                new DijkstraRoute.Stop(2L, 4.61, -74.08),
                new DijkstraRoute.Stop(3L, 4.62, -74.08))));
    }

    @Test void unaParada() {
        assertEquals(List.of(1L), DijkstraRoute.order(List.of(new DijkstraRoute.Stop(1L, 4.6, -74.0))));
    }

    @Test void sinEntregas() {
        assertTrue(DijkstraRoute.order(List.of()).isEmpty());
    }

    @Test void paradaInalcanzable() {
        Map<Long, List<DijkstraRoute.Edge>> graph = Map.of(
                1L, List.of(new DijkstraRoute.Edge(1L, 2L, 1)),
                2L, List.of(), 3L, List.of());
        assertTrue(DijkstraRoute.shortestPath(1L, 3L, graph).isEmpty());
    }
}
