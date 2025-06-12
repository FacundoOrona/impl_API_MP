package com.api.mp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.mp.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}