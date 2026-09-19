package br.com.erudio.integrationtests

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.Assertions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class AuthenticatedIntegrationTest : AbstractIntegrationTest() {

    companion object {

        private var accessToken: String? = null

        @JvmStatic
        protected fun authenticated(): RequestSpecification {
            return RequestSpecBuilder()
                .setPort(TestConfigs.SERVER_PORT)
                .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + accessToken())
                .build()
        }

        @JvmStatic
        protected fun anonymous(): RequestSpecification {
            return RequestSpecBuilder()
                .setPort(TestConfigs.SERVER_PORT)
                .build()
        }

        @Synchronized
        private fun accessToken(): String {
            if (accessToken == null) {
                accessToken = given()
                    .port(TestConfigs.SERVER_PORT)
                    .basePath("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(AccountCredentialsDTO("leandro", "admin123"))
                    .`when`()
                    .post()
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("accessToken")
                Assertions.assertNotNull(accessToken, "signin did not return an access token")
            }
            return accessToken!!
        }
    }
}
