package com.api.mp.entities;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class StateOauth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @NotNull
    Long usuarioId;
    
    @NotNull
    String state;
    LocalDateTime creado = LocalDateTime.now();
    
    public StateOauth(Long usuarioId, String state){
        this.usuarioId = usuarioId;
        this.state = state;
        this.creado = LocalDateTime.now();
    }
}
