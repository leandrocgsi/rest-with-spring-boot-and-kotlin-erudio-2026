package br.com.erudio.exception.handler

import br.com.erudio.exception.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.util.*

@ControllerAdvice
@RestController
class CustomEntityResponseHandler : ResponseEntityExceptionHandler() {

    override fun createResponseEntity(
        body: Any?, headers: HttpHeaders, statusCode: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any> {
        if (body is ProblemDetail && body.type == null) {
            body.type = URI.create("about:blank")
        }
        return super.createResponseEntity(body, headers, statusCode, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFoundExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(RequiredObjectIsNullException::class)
    fun handleRequiredObjectExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFoundExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorageExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(InvalidJwtAuthenticationException::class)
    fun handleInvalidJwtAuthenticationExceptions(ex: Exception, request: WebRequest): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(Date(), ex.message, request.getDescription(false))
        return ResponseEntity(response, HttpStatus.FORBIDDEN)
    }
}
