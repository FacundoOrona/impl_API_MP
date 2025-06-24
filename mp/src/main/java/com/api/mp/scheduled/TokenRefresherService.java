package com.api.mp.scheduled;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.api.mp.entities.OauthToken;
import com.api.mp.entities.OauthTokenRequestDTO;
import com.api.mp.service.MPService;
import com.api.mp.service.OauthService;
//import com.api.mp.util.EncriptadoUtil;

@Service
public class TokenRefresherService {
    
    private final MPService mercadoPagoService;
    private final OauthService oauthService;
    //private final EncriptadoUtil encriptadoUtil;


}

