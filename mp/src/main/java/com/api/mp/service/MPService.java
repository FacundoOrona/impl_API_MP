package com.api.mp.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.api.mp.entities.*;
import com.api.mp.repository.*;

@Service
public class MPService {

        // @Value("${mercadopago.access-token}")
        // String accessToken;

        @Value("${clientId}")
        String clientId;

        @Value("${clientSecret}")
        String clientSecret;

        private final ProductoRepository productoRepository;
        private final TransaccionRepository transaccionRepository;
        private final UsuarioRepository usuarioRepository;
        private final OauthService oauthService;

        public MPService(ProductoRepository p, TransaccionRepository t, UsuarioRepository u,
                        OauthService oauthService) {
                this.productoRepository = p;
                this.transaccionRepository = t;
                this.usuarioRepository = u;
                this.oauthService = oauthService;
        }

        public String crearPreferencia(ProductoRequestDTO p) throws Exception {
                // Se busca el producto recibido
                Producto producto = productoRepository.findById(p.getId())
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                if (!producto.estaDisponible()) {
                        throw new RuntimeException("Producto vendido o reservado");
                }

                String accessToken = oauthService.obtenerAccessTokenPorId(1L);

                if (!oauthService.AccessTokenValido(accessToken)) {
                        throw new RuntimeException("Access token vencido o revocado por el vendedor");
                }

                // Inicializa config
                MercadoPagoConfig.setAccessToken(accessToken);

                // Crea el ítem
                PreferenceItemRequest item = PreferenceItemRequest.builder()
                                .title(producto.getNombre())
                                .quantity(1)
                                .currencyId("ARS")
                                .unitPrice(new BigDecimal(producto.getPrecio()))
                                .build();

                // ACA CAMBIAR y Obtener el usuario logueado el cual va a comprar
                Usuario usuarioComprador = usuarioRepository.findById(new Long("1"))
                                .orElseThrow(() -> new RuntimeException("usuario no encontrado"));

                // Se crea la transaccion en la base de datos y se obtiene la misma guardada con
                // su id
                Transaccion transaccion = new Transaccion("Pendiente", usuarioComprador, producto);
                Transaccion transaccionSave = transaccionRepository.save(transaccion);

                // Tiempo actual
                OffsetDateTime now = OffsetDateTime.now();

                // Tiempo de expiración: 2 minutos desde ahora
                OffsetDateTime expirationFrom = now;
                OffsetDateTime expirationTo = now.plusMinutes(2);

                // Arma la preferencia
                PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                                .items(List.of(item))
                                // Aca se manda el id de la transaccion para obtenerlo cuando se haga el pago
                                .externalReference(transaccionSave.getId().toString())
                                // Aca se setean datos para que la URL expire y no sea comprada mas alla de lo
                                // que dura la reserva
                                .expires(true)
                                .expirationDateFrom(expirationFrom)
                                .expirationDateTo(expirationTo)
                                .build();

                // Se marca el producto como reservado
                producto.setReservado(true);
                producto.setFecha_reserva(LocalDateTime.now());
                productoRepository.save(producto);

                // Se termina la preferencia
                PreferenceClient client = new PreferenceClient();
                Preference preference = client.create(preferenceRequest);

                // Retorna la URL de pago
                return preference.getInitPoint();
        }

        public void procesarWebhook(WebhookDTO webhook) {
                if (!"payment".equalsIgnoreCase(webhook.getType())) {
                        System.out.println("Webhook ignorado: tipo no soportado " + webhook.getType());
                        return;
                }
                try {
                        // Obtengo el ID de pago
                        String paymentId = webhook.getData().getId();

                        // Con el id obtenido busco el pago
                        PaymentClient client = new PaymentClient();
                        Payment payment = client.get(Long.parseLong(paymentId));

                        // obtengo el stado del pafo
                        String estado = payment.getStatus();

                        // Obtengo el ID del la transaccion para cambiuarte el estado
                        String externalReference = payment.getExternalReference();

                        Transaccion tr = transaccionRepository.findById(Long.parseLong(externalReference))
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Transacción no encontrada: ID " + externalReference));

                        Producto pr = productoRepository.findById(tr.getProducto().getId())
                                        .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID "
                                                        + tr.getProducto().getId()));

                        if (pr.isVendido()) {
                                System.out.println("El producto ya fue vendido, no se puede cambiar");
                                return;
                        }

                        pr.setVendido(true);
                        productoRepository.save(pr);

                        if (externalReference == null) {
                                System.out.println("No se encontró externalReference (transactionId)");
                                return;
                        }
                        // Se castea a Long se va a buscar la transaccion y se le seteaa el nuevo estado
                        Long transactionId = Long.parseLong(externalReference);
                        Transaccion transaccion = transaccionRepository.findById(transactionId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Transacción no encontrada: ID " + transactionId));
                        transaccion.setEstado(estado);
                        transaccionRepository.save(transaccion);
                } catch (Exception e) {
                        System.out.println("Error al procesar webhook: " + e.getMessage());
                }
        }
}