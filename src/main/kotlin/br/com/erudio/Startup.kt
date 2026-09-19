package br.com.erudio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder

@SpringBootApplication
class Startup

fun main(args: Array<String>) {
    runApplication<Startup>(*args)
}

@Suppress("unused")
private fun generateHashedPassword() {

    val pbkdf2Encoder = Pbkdf2PasswordEncoder(
        "", 8, 185000,
        Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
    )

    val encoders: MutableMap<String, PasswordEncoder> = HashMap()
    encoders["pbkdf2"] = pbkdf2Encoder
    val passwordEncoder = DelegatingPasswordEncoder("pbkdf2", encoders)

    passwordEncoder.setDefaultPasswordEncoderForMatches(pbkdf2Encoder)
    val pass1 = passwordEncoder.encode("admin123")
    val pass2 = passwordEncoder.encode("admin234")

    println(pass1)
    println(pass2)
}
