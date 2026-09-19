package br.com.erudio.services

import br.com.erudio.data.dto.security.AccountCredentialsDTO
import br.com.erudio.data.dto.security.TokenDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.model.User
import br.com.erudio.repository.UserRepository
import br.com.erudio.security.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class AuthService {

    private val logger = Logger.getLogger(AuthService::class.java.name)

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var tokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var repository: UserRepository

    fun signIn(credentials: AccountCredentialsDTO): ResponseEntity<TokenDTO> {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                credentials.username,
                credentials.password
            )
        )

        val user = repository.findByUsername(credentials.username)
            ?: throw UsernameNotFoundException("Username ${credentials.username} not found!")

        val token = tokenProvider.createAccessToken(
            credentials.username!!,
            user.roles
        )
        return ResponseEntity.ok(token)
    }

    fun refreshToken(username: String, refreshToken: String): ResponseEntity<TokenDTO> {
        val user = repository.findByUsername(username)
        val token: TokenDTO = if (user != null) {
            tokenProvider.refreshToken(refreshToken)
        } else {
            throw UsernameNotFoundException("Username $username not found!")
        }
        return ResponseEntity.ok(token)
    }

    fun create(user: AccountCredentialsDTO?): AccountCredentialsDTO {

        if (user == null) throw RequiredObjectIsNullException()

        logger.info("Creating one new User!")
        val entity = User()
        entity.fullName = user.fullname
        entity.userName = user.username
        entity.setPassword(generateHashedPassword(user.password!!))
        entity.accountNonExpired = true
        entity.accountNonLocked = true
        entity.credentialsNonExpired = true
        entity.enabled = true

        val dto = repository.save(entity)
        return AccountCredentialsDTO(dto.username, dto.password, dto.fullName)
    }

    private fun generateHashedPassword(password: String): String {

        val pbkdf2Encoder = Pbkdf2PasswordEncoder(
            "", 8, 185000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        )

        val encoders: MutableMap<String, PasswordEncoder> = HashMap()
        encoders["pbkdf2"] = pbkdf2Encoder
        val passwordEncoder = DelegatingPasswordEncoder("pbkdf2", encoders)

        passwordEncoder.setDefaultPasswordEncoderForMatches(pbkdf2Encoder)
        return passwordEncoder.encode(password)!!
    }
}
