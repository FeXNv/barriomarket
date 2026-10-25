package com.fexsolutions.barriomarket.security;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Cuenta intentos de login fallidos por teléfono. El conteo se olvida solo pasada la ventana. */
@Component
public class LoginThrottle {

    private final int maxIntentos;
    private final Cache<String, Integer> fallos;

    @Autowired
    public LoginThrottle() {
        this(5, Duration.ofMinutes(15), Ticker.systemTicker());
    }

    public LoginThrottle(int maxIntentos, Duration ventana, Ticker reloj) {
        this.maxIntentos = maxIntentos;
        this.fallos = CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(ventana)
                .ticker(reloj)
                .build();
    }

    public boolean bloqueado(String clave) {
        Integer n = fallos.getIfPresent(clave);
        return n != null && n >= maxIntentos;
    }

    public void registrarFallo(String clave) {
        fallos.asMap().merge(clave, 1, Integer::sum);
    }

    public void limpiar(String clave) {
        fallos.invalidate(clave);
    }
}