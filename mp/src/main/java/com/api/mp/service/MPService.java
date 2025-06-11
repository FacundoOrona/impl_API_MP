package com.api.mp.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.api.mp.entities.Producto;
import com.api.mp.entities.ProductoRequestDTO;

@Service
public class MPService {

    @Value("${mercadopago.access-token}")
    String accessToken;

    public String crearPreferencia(ProductoRequestDTO p) throws Exception {

        Producto producto = new Producto();

        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(producto.getTitulo())
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(producto.getPrecio())
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .build();

        PreferenceClient preferenceClient = new PreferenceClient();
        Preference preference = preferenceClient.create(preferenceRequest);
        
        return preference.getInitPoint();
    }
}
