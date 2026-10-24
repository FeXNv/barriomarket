package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final RecordatorioRepository recordatorios;

    public void sincronizar(Producto producto) { throw new UnsupportedOperationException(); }
}