package com.udea.demo.pedidos.domain.service;

import com.udea.demo.pedidos.domain.exception.UbicacionInvalidaException;
import com.udea.demo.pedidos.domain.model.UbicacionReportada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rechaza coordenadas imposibles y marca como no confiables las ubicaciones nulas
 * o cuya precisión declarada por el dispositivo supera el margen permitido.
 */
@Component
public class ValidadorUbicacion {
    private final double precisionMaximaMetros;

    public ValidadorUbicacion(@Value("${app.checkpoints.precision-maxima-metros:100}") double precisionMaximaMetros) {
        this.precisionMaximaMetros = precisionMaximaMetros;
    }

    public UbicacionReportada evaluar(Double latitud, Double longitud, Double precisionMetros) {
        if ((latitud == null) != (longitud == null))
            throw new UbicacionInvalidaException("latitud y longitud deben reportarse juntas");
        if (latitud != null && (!Double.isFinite(latitud) || latitud < -90 || latitud > 90))
            throw new UbicacionInvalidaException("la latitud debe estar entre -90 y 90");
        if (longitud != null && (!Double.isFinite(longitud) || longitud < -180 || longitud > 180))
            throw new UbicacionInvalidaException("la longitud debe estar entre -180 y 180");
        if (precisionMetros != null && (!Double.isFinite(precisionMetros) || precisionMetros < 0))
            throw new UbicacionInvalidaException("la precisión debe ser un valor positivo en metros");
        boolean confiable = latitud != null && precisionMetros != null && precisionMetros <= precisionMaximaMetros;
        return new UbicacionReportada(latitud, longitud, precisionMetros, confiable);
    }
}
