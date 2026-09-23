package com.udea.demo.pedidos.application.service;

import com.udea.demo.pedidos.application.dto.RecibirPedidoRequestDTO;
import com.udea.demo.pedidos.domain.model.TipoServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LimitesServicioService {
    @Value("${app.shipment.max-weight-kg:50}") private double maxWeightKg;
    @Value("${app.shipment.max-dimension-cm:120}") private double maxDimensionCm;
    @Value("${app.shipment.max-volume-cm3:1000000}") private double maxVolumeCm3;
    @Value("${app.shipment.express.max-weight-kg:20}") private double expressMaxWeightKg;
    @Value("${app.shipment.express.max-dimension-cm:80}") private double expressMaxDimensionCm;
    @Value("${app.shipment.express.max-volume-cm3:500000}") private double expressMaxVolumeCm3;

    public void validar(RecibirPedidoRequestDTO dto) {
        if (dto.tipoServicio() == null) throw new IllegalArgumentException("El tipo de servicio es obligatorio");
        validarPositivo(dto.pesoKg(), "peso"); validarPositivo(dto.largoCm(), "largo");
        validarPositivo(dto.anchoCm(), "ancho"); validarPositivo(dto.altoCm(), "alto");
        double pesoLimite = dto.tipoServicio() == TipoServicio.EXPRESS ? Math.min(maxWeightKg, expressMaxWeightKg) : maxWeightKg;
        double dimensionLimite = dto.tipoServicio() == TipoServicio.EXPRESS ? Math.min(maxDimensionCm, expressMaxDimensionCm) : maxDimensionCm;
        double volumenLimite = dto.tipoServicio() == TipoServicio.EXPRESS ? Math.min(maxVolumeCm3, expressMaxVolumeCm3) : maxVolumeCm3;
        if (dto.pesoKg() > pesoLimite) throw new IllegalArgumentException("El peso supera el máximo de " + pesoLimite + " kg para " + dto.tipoServicio());
        if (dto.largoCm() > dimensionLimite || dto.anchoCm() > dimensionLimite || dto.altoCm() > dimensionLimite)
            throw new IllegalArgumentException("Una dimensión supera el máximo de " + dimensionLimite + " cm para " + dto.tipoServicio());
        if (dto.largoCm() * dto.anchoCm() * dto.altoCm() > volumenLimite)
            throw new IllegalArgumentException("El volumen supera el límite de " + volumenLimite + " cm³ para " + dto.tipoServicio());
        if (dto.direccionOrigen() == null || dto.direccionOrigen().trim().length() < 8)
            throw new IllegalArgumentException("La dirección de origen debe incluir calle y número, o una referencia rural suficiente");
        if (dto.direccionDestino() == null || dto.direccionDestino().trim().length() < 8)
            throw new IllegalArgumentException("La dirección de destino debe incluir calle y número, o una referencia rural suficiente");
        if (dto.ciudadOrigen() == null || dto.ciudadOrigen().isBlank() || dto.ciudadDestino() == null || dto.ciudadDestino().isBlank())
            throw new IllegalArgumentException("Indica la ciudad de origen y destino");
    }

    private void validarPositivo(Double value, String nombre) {
        if (value == null || !Double.isFinite(value) || value <= 0)
            throw new IllegalArgumentException("El " + nombre + " debe ser mayor que cero");
    }
}
