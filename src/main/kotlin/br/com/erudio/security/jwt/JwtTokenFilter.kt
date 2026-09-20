package br.com.erudio.security.jwt

import br.com.erudio.exception.InvalidJwtAuthenticationException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean

class JwtTokenFilter(private val tokenProvider: JwtTokenProvider) : GenericFilterBean() {

    override fun doFilter(request: ServletRequest, response: ServletResponse, filter: FilterChain) {
        val token = tokenProvider.resolveToken(request as HttpServletRequest)
        if (!token.isNullOrBlank() && isValid(token)) {
            val authentication = tokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }
        filter.doFilter(request, response)
    }

    private fun isValid(token: String): Boolean {
        return try {
            tokenProvider.validateToken(token)
        } catch (e: InvalidJwtAuthenticationException) {
            false
        }
    }
}
