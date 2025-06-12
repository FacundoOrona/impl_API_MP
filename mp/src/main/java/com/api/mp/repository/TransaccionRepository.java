package com.api.mp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.mp.entities.Transaccion;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    
}
