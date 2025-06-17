package com.api.mp.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.api.mp.entities.StateOauth;

public interface StateOauthRepository extends JpaRepository<StateOauth, Long>{
    Optional<StateOauth> findByState(String state);
}
