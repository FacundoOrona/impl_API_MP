package com.api.mp.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.api.mp.entities.Usuario;
import com.api.mp.repository.UsuarioRepository;

@Service
public class UsuarioService {
  
    private final UsuarioRepository usuarioRepository;
    private final StateOauthService stateOauthService;

    public UsuarioService(UsuarioRepository usuarioRepository, StateOauthService stateOauthService) {
        this.usuarioRepository = usuarioRepository;
        this.stateOauthService = stateOauthService;
    }

    public Optional<Usuario> obtenerUsuariosPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario obtenerUsuarioPorState(String state) {
        Long id = stateOauthService.obtenerIdUsuarioPorState(state);
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado por state"));
    }
}
