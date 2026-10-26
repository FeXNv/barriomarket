package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ActualizacionStockResponse;
import com.fexsolutions.barriomarket.repository.HistorialStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorialService {

    private static final int LIMITE_POR_DEFECTO = 10;
    private static final int LIMITE_MAXIMO = 50;

    private final HistorialStockRepository historial;

    public List<ActualizacionStockResponse> ultimas(Integer idUsuario, Integer limite) {
        int n = limite == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        return historial.ultimasDelUsuario(idUsuario, PageRequest.of(0, n));
    }
}