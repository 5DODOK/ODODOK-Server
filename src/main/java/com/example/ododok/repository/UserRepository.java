package com.example.ododok.repository;

import com.example.ododok.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
    Optional<User> findByEmail(String email);
    boolean existsByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}