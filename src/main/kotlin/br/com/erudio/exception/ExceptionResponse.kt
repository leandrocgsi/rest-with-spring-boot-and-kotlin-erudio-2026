package br.com.erudio.exception

import java.util.*

data class ExceptionResponse(
    val timestamp: Date,
    val message: String?,
    val details: String
)
