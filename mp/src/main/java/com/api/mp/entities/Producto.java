package com.api.mp.entities;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class Producto {
    private int id;
    private String titulo;
    private BigDecimal precio;

    public Producto(){
        this.id = 1;
        this.titulo = "Camiseta de River";
        this.precio = new BigDecimal("100.00");
    }
}
