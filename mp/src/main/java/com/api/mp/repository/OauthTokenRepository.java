package com.api.mp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.mp.entities.OauthToken;

public interface OauthTokenRepository extends JpaRepository<OauthToken, Long>{
    
}
