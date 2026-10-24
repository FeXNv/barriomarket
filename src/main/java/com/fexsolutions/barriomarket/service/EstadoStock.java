package com.fexsolutions.barriomarket.service;

public enum EstadoStock {
    VERDE("verde"), AMARILLO("amarillo"), ROJO("rojo");

    private final String valor;

    EstadoStock(String valor) { this.valor = valor; }

    public String getValor() { return valor; }
}