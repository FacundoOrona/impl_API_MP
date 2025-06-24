package com.api.mp.scheduled;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.api.mp.entities.OauthToken;
import com.api.mp.entities.OauthTokenRequestDTO;
import com.api.mp.service.MPService;
import com.api.mp.service.OauthService;
import com.api.mp.util.EncriptadoUtil;

@Service
public class TokenRefresherService {
    
    private final MPService mercadoPagoService;
    private final OauthService oauthService;
    private final EncriptadoUtil encriptadoUtil;

    public TokenRefresherService(MPService mercadoPagoService, OauthService oauthService, EncriptadoUtil encriptadoUtil){
        this.mercadoPagoService = mercadoPagoService;
        this.oauthService = oauthService;
        this.encriptadoUtil = encriptadoUtil;
    }

    @Scheduled(fixedRate = 3600000) // cada 1 hora (en milisegundos)
    public void refrescarTokens() {
        List<OauthToken> tokens = oauthService.obtenerTokenDeUsuariosVendedores();
        for (OauthToken token : tokens) {
            if (tokenExpirado(token)) {
                try {
                    OauthTokenRequestDTO nuevoToken = mercadoPagoService.refrescarToken(encriptadoUtil.desencriptar(token.getRefreshToken()));
                    oauthService.actualizarToken(nuevoToken, token.getUsuario());
                } catch (Exception e) {
                    System.err.println("Error actualizando token para usuario " + token.getUsuario().getNombre());
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    private boolean tokenExpirado(OauthToken token) {
        return token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(11)); // margen de seguridad
    }

}

