package com.api.mp.scheduled;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.api.mp.entities.Producto;
import com.api.mp.repository.ProductoRepository;

@Component
public class ProductoScheduled {
    
    private final ProductoRepository productoRepository;

    public ProductoScheduled(ProductoRepository productoRepository){
        this.productoRepository = productoRepository;
    }

    @Scheduled(fixedRate = 60000) //cada un minuto
    public void liberarProductos() {
        List<Producto> productos = productoRepository.findByReservadoTrue();

        for (Producto p : productos) {
            if(p.getFecha_reserva().isBefore(LocalDateTime.now().minusMinutes(2))) {
                p.setReservado(false);
                productoRepository.save(p);
            }
        }
    }
}
