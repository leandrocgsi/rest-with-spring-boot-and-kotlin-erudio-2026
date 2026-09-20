package br.com.erudio.integrationtests.controllers.security

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.AuthenticatedIntegrationTest
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import java.util.Base64
import java.util.Date

class InvalidTokenTest : AuthenticatedIntegrationTest() {

    @Value("\${security.jwt.token.secret-key}")
    private lateinit var secretKey: String

    private fun token(secret: String, expiresAt: Date): String {
        val key = Base64.getEncoder().encodeToString(secret.toByteArray())
        return JWT.create()
            .withSubject("leandro")
            .withClaim("roles", listOf("ADMIN"))
            .withIssuedAt(Date(expiresAt.time - 3_600_000))
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(key.toByteArray()))
    }

    private fun expiredToken(): String = token(secretKey, Date(System.currentTimeMillis() - 60_000))

    private fun assertRejected(bearer: String) {
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer $bearer")
            .`when`()
            .get("/api/person/v1")
            .then()
            .statusCode(403)
            .body(emptyString())
    }

    private fun refreshRequest(authorization: String) = given().spec(anonymous())
        .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, authorization)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .`when`()
        .put("/auth/refresh/leandro")
        .then()

    private fun assertRefreshRejected(authorization: String) {
        refreshRequest(authorization)
            .statusCode(403)
            .body("message", equalTo("Expired or Invalid JWT Token!"))
    }

    private fun signin(): String {
        return given().spec(anonymous())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(AccountCredentialsDTO("leandro", "admin123"))
            .`when`()
            .post("/auth/signin")
            .then()
            .statusCode(200)
            .extract()
            .path("refreshToken")
    }

    @Test
    fun anExpiredTokenIsRejectedLikeAMissingOne() {
        assertRejected(expiredToken())
    }

    @Test
    fun aTokenSignedWithAnotherKeyIsRejected() {
        assertRejected(token("another-secret", Date(System.currentTimeMillis() + 60_000)))
    }

    @Test
    fun aMalformedTokenIsRejected() {
        assertRejected("not-a-jwt")
    }

    @Test
    fun aValidTokenIsStillAccepted() {
        given().spec(authenticated())
            .`when`()
            .get("/api/person/v1")
            .then()
            .statusCode(200)
    }

    @Test
    fun anInvalidTokenDoesNotBlockSignin() {
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${expiredToken()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(AccountCredentialsDTO("leandro", "admin123"))
            .`when`()
            .post("/auth/signin")
            .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
    }

    @Test
    fun anInvalidTokenDoesNotBlockThePublicDocumentation() {
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${expiredToken()}")
            .`when`()
            .get("/v3/api-docs")
            .then()
            .statusCode(200)
    }

    @Test
    fun anExpiredRefreshTokenIsRejected() {
        assertRefreshRejected("Bearer ${expiredToken()}")
    }

    @Test
    fun aMalformedRefreshTokenIsRejected() {
        assertRefreshRejected("Bearer not-a-jwt")
    }

    @Test
    fun aRefreshTokenWithoutTheBearerPrefixIsRejected() {
        assertRefreshRejected(signin())
    }

    @Test
    fun aValidRefreshTokenStillWorks() {
        refreshRequest("Bearer ${signin()}")
            .statusCode(200)
            .body("accessToken", notNullValue())
    }
}
