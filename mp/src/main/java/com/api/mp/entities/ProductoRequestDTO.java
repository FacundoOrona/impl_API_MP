package com.api.mp.entities;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequestDTO {
    @NotNull(message = "El ID no puede estar vacio o nulo")
    Long id;
}
