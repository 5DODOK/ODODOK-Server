package com.example.ododok.service;

import com.example.ododok.entity.User;
import com.example.ododok.entity.UserRole;
import com.example.ododok.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        User user = processOAuth2User(registrationId, attributes);
        
        return new CustomOAuth2User(oAuth2User, user);
    }

    private User processOAuth2User(String registrationId, Map<String, Object> attributes) {
        String oauthId = String.valueOf(attributes.get("sub"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        User existingUser = userRepository.findByOauthProviderAndOauthId(registrationId, oauthId)
                .orElse(null);

        if (existingUser != null) {
            existingUser.setName(name);
            existingUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existingUser);
        } else {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setOauthProvider(registrationId);
            newUser.setOauthId(oauthId);
            newUser.setRole(determineUserRole(email));
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            return userRepository.save(newUser);
        }
    }

    private UserRole determineUserRole(String email) {
        return email.endsWith("@bssm.hs.kr") ? UserRole.ADMIN : UserRole.USER;
    }
}