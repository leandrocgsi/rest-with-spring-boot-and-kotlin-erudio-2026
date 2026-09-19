package br.com.erudio.controllers

import br.com.erudio.controllers.docs.AuthControllerDocs
import br.com.erudio.data.dto.security.AccountCredentialsDTO
import br.com.erudio.services.AuthService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Authentication Endpoint!")
@RestController
@RequestMapping("/auth")
class AuthController : AuthControllerDocs {

    @Autowired
    private lateinit var service: AuthService

    @PostMapping("/signin")
    override fun signin(@RequestBody credentials: AccountCredentialsDTO): ResponseEntity<*> {
        if (credentialsIsInvalid(credentials))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid client request!")
        return service.signIn(credentials)
    }

    @PutMapping("/refresh/{username}")
    override fun refreshToken(
        @PathVariable("username") username: String,
        @RequestHeader("Authorization") refreshToken: String
    ): ResponseEntity<*> {
        if (parametersAreInvalid(username, refreshToken))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid client request!")
        return service.refreshToken(username, refreshToken)
    }

    @PostMapping(
        value = ["/createUser"],
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun create(@RequestBody credentials: AccountCredentialsDTO): AccountCredentialsDTO {
        return service.create(credentials)
    }

    private fun parametersAreInvalid(username: String, refreshToken: String): Boolean {
        return username.isBlank() || refreshToken.isBlank()
    }

    private fun credentialsIsInvalid(credentials: AccountCredentialsDTO): Boolean {
        return credentials.password.isNullOrBlank() || credentials.username.isNullOrBlank()
    }
}
