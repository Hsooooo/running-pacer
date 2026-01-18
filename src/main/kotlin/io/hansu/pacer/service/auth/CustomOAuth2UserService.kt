package io.hansu.pacer.service.auth

import io.hansu.pacer.domain.user.UserEntity
import io.hansu.pacer.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)
        
        val provider = userRequest.clientRegistration.registrationId.uppercase()
        val attributes = oauth2User.attributes
        
        // Extract info based on provider (simplified for Google/Common)
        val providerId = attributes["sub"] as? String ?: attributes["id"]?.toString() ?: throw IllegalArgumentException("Missing provider ID")
        val email = attributes["email"] as? String
        val name = attributes["name"] as? String ?: "User"
        
        // Find existing user by Email (priority) or Provider ID
        var user = if (email != null) userRepository.findByEmail(email) else null
        if (user == null) {
            user = userRepository.findByProviderAndProviderId(provider, providerId)
        }
            
        if (user == null) {
            user = UserEntity(
                nickname = name,
                email = email,
                provider = provider,
                providerId = providerId
            )
            user = userRepository.save(user)
        }
        
        // Return wrapped user with userId in attributes for easy access
        val newAttributes = HashMap(attributes)
        newAttributes["userId"] = user.id
        
        val userNameAttributeName = userRequest.clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName
            ?: "sub"

        return DefaultOAuth2User(
            oauth2User.authorities,
            newAttributes,
            userNameAttributeName
        )
    }
}
