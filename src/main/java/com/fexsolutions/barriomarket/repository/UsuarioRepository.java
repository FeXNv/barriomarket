package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByTelefonoAndDeletedAtIsNull(String telefono);
    boolean existsByTelefono(String telefono);
    boolean existsByEmail(String email);
}