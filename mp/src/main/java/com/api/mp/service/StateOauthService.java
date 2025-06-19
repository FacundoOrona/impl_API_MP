package com.api.mp.service;

import com.api.mp.entities.StateOauth;
import com.api.mp.repository.StateOauthRepository;
import org.springframework.stereotype.Service;

@Service
public class StateOauthService {
    
    private final StateOauthRepository stateOauthRepository;

    public StateOauthService(StateOauthRepository stateOauthRepository) {
        this.stateOauthRepository = stateOauthRepository;
    }

    public void guardarStateOauth(Long idUsuarioLogueado, String state) {
        StateOauth entity = new StateOauth(idUsuarioLogueado, state);
        stateOauthRepository.save(entity);
    }

    public Long obtenerIdUsuarioPorState(String state) {
        return stateOauthRepository.findByState(state)
                .orElseThrow(() -> new RuntimeException("Stateno encontrado"))
                .getId(); 
    }
}
