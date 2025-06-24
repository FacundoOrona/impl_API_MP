package com.api.mp.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import com.api.mp.exceptions.TokenRevocadoException;
import com.api.mp.entities.*;
import com.api.mp.repository.*;
import com.api.mp.util.EncriptadoUtil;

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
        private final EncriptadoUtil encriptadoUtil;

        public MPService(ProductoRepository p, TransaccionRepository t, UsuarioRepository u,
                        OauthService oauthService, EncriptadoUtil e) {
                this.productoRepository = p;
                this.transaccionRepository = t;
                this.usuarioRepository = u;
                this.oauthService = oauthService;
                this.encriptadoUtil = e;
        }

        public String crearPreferencia(ProductoRequestDTO p) throws Exception {
                // Se busca el producto recibido
                Producto producto = productoRepository.findById(p.getId())
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                if (!producto.estaDisponible()) {
                        throw new RuntimeException("Producto vendido o reservado");
                }

                String accessTokenEncriptado = oauthService.obtenerAccessTokenPorId(1L);
                String accessToken = encriptadoUtil.desencriptar(accessTokenEncriptado);

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

                        Long transactionId = Long.parseLong(externalReference);
                        Transaccion transaccion = transaccionRepository.findById(transactionId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Transacción no encontrada: ID " + transactionId));

                        // Obtengo el producto de la transaccion
                        Producto producto = transaccion.getProducto();

                        // Obtengo el accessToken del vendedor por si hay que rembolsar
                        String accessTokenEncriptado = oauthService.obtenerAccessTokenPorId(1L);
                        String accessToken = encriptadoUtil.desencriptar(accessTokenEncriptado);

                        if (externalReference == null) {
                                System.out.println("No se encontró externalReference (transactionId)");
                                reembolsarPago(paymentId, accessToken);
                                return;
                        }

                        if (producto.getVendido()) { //isVendido si es boolean con b minus
                                // en caso que se haya venido se reembolsa
                                System.out.println("Producto ya no está disponible, haciendo reembolso...");
                                reembolsarPago(paymentId, accessToken);
                                transaccion.setEstado("reembolsado");
                                return;
                        }

                        if ("approved".equalsIgnoreCase(estado)) {
                                producto.setVendido(true);
                                transaccion.setEstado("Pago");
                                productoRepository.save(producto);
                                transaccionRepository.save(transaccion);
                        }
                } catch (Exception e) {
                        System.out.println("Error al procesar webhook: " + e.getMessage());
                }
        }

        private void reembolsarPago(String paymentId, String accessToken) {
                try {
                        MercadoPagoConfig.setAccessToken(accessToken);
                        // Obtener el pago con PaymentClient
                        PaymentClient client = new PaymentClient();
                        Payment payment = client.get(Long.parseLong(paymentId));
                        // Ejecutar el reembolso
                        PaymentRefund refundPayment = client.refund(payment.getId());

                        System.out.println("Reembolso exitoso: " + refundPayment.getId());

                } catch (MPApiException e) {
                        System.out.println("Error MercadoPago: " + e.getApiResponse().getContent());
                } catch (Exception e) {
                        System.out.println("Error inesperado: " + e.getMessage());
                }
        }

        public OauthTokenRequestDTO refrescarToken(String refreshToken) {
                try {
                        RestTemplate restTemplate = new RestTemplate();

                        String url = "https://api.mercadopago.com/oauth/token";

                        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                        body.add("grant_type", "refresh_token");
                        body.add("client_id", clientId);
                        body.add("client_secret", clientSecret);
                        body.add("refresh_token", refreshToken);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                        ResponseEntity<OauthTokenRequestDTO> response = restTemplate.postForEntity(url, request,
                                        OauthTokenRequestDTO.class);

                        return response.getBody();
                } catch (HttpClientErrorException e) {
                        if (e.getStatusCode() == HttpStatus.BAD_REQUEST
                                        || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                                throw new TokenRevocadoException("El refresh token fue revocado o no es válido");
                        }
                        throw e;
                }
        }
}