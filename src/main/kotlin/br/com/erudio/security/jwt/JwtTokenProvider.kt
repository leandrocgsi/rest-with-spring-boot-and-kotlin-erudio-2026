package br.com.erudio.security.jwt

import br.com.erudio.data.dto.security.TokenDTO
import br.com.erudio.exception.InvalidJwtAuthenticationException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*

@Service
class JwtTokenProvider {

    @Value("\${security.jwt.token.secret-key:secret}")
    private var secretKey = "secret"

    @Value("\${security.jwt.token.expire-length:3600000}")
    private var validityInMilliseconds: Long = 3_600_000

    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    private lateinit var algorithm: Algorithm

    @PostConstruct
    protected fun init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.toByteArray())
        algorithm = Algorithm.HMAC256(secretKey.toByteArray())
    }

    fun createAccessToken(username: String, roles: List<String?>): TokenDTO {
        val now = Date()
        val validity = Date(now.time + validityInMilliseconds)
        val accessToken = getAccessToken(username, roles, now, validity)
        val refreshToken = getRefreshToken(username, roles, now)
        return TokenDTO(
            username = username,
            authenticated = true,
            created = now,
            expiration = validity,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun refreshToken(refreshToken: String): TokenDTO {
        var token = ""
        if (refreshTokenContainsBearer(refreshToken)) {
            token = refreshToken.substring("Bearer ".length)
        }

        val verifier = JWT.require(algorithm).build()
        val decodedJWT = try {
            verifier.verify(token)
        } catch (e: JWTVerificationException) {
            throw InvalidJwtAuthenticationException("Expired or Invalid JWT Token!")
        }

        val username = decodedJWT.subject
        val roles: List<String?> = decodedJWT.getClaim("roles").asList(String::class.java)
        return createAccessToken(username, roles)
    }

    private fun getRefreshToken(username: String, roles: List<String?>, now: Date): String {
        val refreshTokenValidity = Date(now.time + (validityInMilliseconds * 3))
        return JWT.create()
            .withClaim("roles", roles)
            .withIssuedAt(now)
            .withExpiresAt(refreshTokenValidity)
            .withSubject(username)
            .sign(algorithm)
    }

    private fun getAccessToken(username: String, roles: List<String?>, now: Date, validity: Date): String {

        val issuerUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        return JWT.create()
            .withClaim("roles", roles)
            .withIssuedAt(now)
            .withExpiresAt(validity)
            .withSubject(username)
            .withIssuer(issuerUrl)
            .sign(algorithm)
    }

    fun getAuthentication(token: String): Authentication {
        val decodedJWT = decodedToken(token)
        val userDetails = userDetailsService.loadUserByUsername(decodedJWT.subject)
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    private fun decodedToken(token: String): DecodedJWT {
        val alg = Algorithm.HMAC256(secretKey.toByteArray())
        val verifier = JWT.require(alg).build()
        return verifier.verify(token)
    }

    fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken: String? = request.getHeader("Authorization")

        if (refreshTokenContainsBearer(bearerToken)) return bearerToken!!.substring("Bearer ".length)
        return null
    }

    private fun refreshTokenContainsBearer(refreshToken: String?): Boolean {
        return !refreshToken.isNullOrBlank() && refreshToken.startsWith("Bearer ")
    }

    fun validateToken(token: String): Boolean {
        try {
            val decodedJWT = decodedToken(token)
            if (decodedJWT.expiresAt.before(Date())) {
                return false
            }
            return true
        } catch (e: Exception) {
            throw InvalidJwtAuthenticationException("Expired or Invalid JWT Token!")
        }
    }
}
