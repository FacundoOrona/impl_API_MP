package com.api.mp.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.api.mp.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}
