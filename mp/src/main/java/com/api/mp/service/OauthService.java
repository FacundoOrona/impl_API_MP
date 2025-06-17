package com.api.mp.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.api.mp.entities.*;
import com.api.mp.repository.*;

@Service
public class OauthService {

    @Value("${clientId}")
    String clientId;

    @Value("${redirectUrl}")
    String redirectUrl;

    @Value("${clientSecret}")
    String clientSecret;

    private final StateOauthRepository stateRepository;
    private final UsuarioRepository usuarioRepository;
    private final OauthTokenRepository oauthRepository;

    public OauthService(StateOauthRepository stateRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, OauthTokenRepository oauthRepository) {
        this.stateRepository = stateRepository;
        this.usuarioRepository = usuarioRepository;
        this.oauthRepository = oauthRepository;
    }

    public String UrlAutorizacion() {
        Long idUsuarioLogueado = new Long(1); // obtiene el id del usuaio autenticado
        String state = UUID.randomUUID().toString(); //guarda state del usuario
        guardarStateOauth(idUsuarioLogueado, state);
        return "https://auth.mercadopago.com.ar/authorization?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUrl +
                "&state=" + state;
    }

    private void guardarStateOauth(Long idUsuario, String state) {
        StateOauth entity = new StateOauth(idUsuario, state);
        stateRepository.save(entity);
    }
}
