package com.api.mp.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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

    private final UsuarioService usuarioService;
    private final OauthTokenRepository oauthRepository;
    private final StateOauthService stateOauthService;

    public OauthService(UsuarioService usuarioService, OauthTokenRepository oauthRepository,
            StateOauthService stateOauthService) {
        this.usuarioService = usuarioService;
        this.oauthRepository = oauthRepository;
        this.stateOauthService = stateOauthService;
    }    

    public String UrlAutorizacion() {
        Long idUsuarioLogueado = usuarioService.obtenerUsuariosPorId(1L) 
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el ID"))
            .getId();  // obtiene el id del usuaio autenticado
        String state = UUID.randomUUID().toString(); // guarda state del usuario
        stateOauthService.guardarStateOauth(idUsuarioLogueado, state);
        return "https://auth.mercadopago.com.ar/authorization?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUrl +
                "&state=" + state;
    }

    public String obtenerAccesToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUrl);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<OauthTokenRequestDTO> response = restTemplate.postForEntity(
                "https://api.mercadopago.com/oauth/token",
                request,
                OauthTokenRequestDTO.class);

        guardarToken(response.getBody(), usuarioService.obtenerUsuarioPorState(state));

        return response.toString();
    }

    public void guardarToken(OauthTokenRequestDTO oauthTokenDTO, Usuario usuario) {
        OauthToken token = new OauthToken();
        token.setAccessToken(oauthTokenDTO.getAccessToken());
        token.setRefreshToken(oauthTokenDTO.getRefreshToken());
        token.setPublicKey(oauthTokenDTO.getPublicKey());
        token.setUserId(oauthTokenDTO.getUserId());
        token.setLiveMode(oauthTokenDTO.isLiveMode());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(oauthTokenDTO.getExpiresIn()));
        token.setUsuario(usuario);

        oauthRepository.save(token);
    }

    public String obtenerAccessTokenPorId(Long id) {
        return oauthRepository.findByUsuarioId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado por ID"))
                .getAccessToken();
    }

    public boolean AccessTokenValido(String accessToken) {
        String url = "https://api.mercadopago.com/users/me";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return false;
            }
            return false;
        }
    }

}
