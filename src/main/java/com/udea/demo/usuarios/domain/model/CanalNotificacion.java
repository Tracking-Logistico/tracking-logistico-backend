package com.udea.demo.usuarios.domain.model;

public enum CanalNotificacion {
    EMAIL, SMS, AMBOS;

    public boolean incluyeEmail() { return this == EMAIL || this == AMBOS; }
    public boolean incluyeSms()   { return this == SMS   || this == AMBOS; }
}