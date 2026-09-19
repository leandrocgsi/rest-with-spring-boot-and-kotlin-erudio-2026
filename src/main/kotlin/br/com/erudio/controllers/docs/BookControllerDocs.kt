package br.com.erudio.controllers.docs

import br.com.erudio.data.dto.BookDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

interface BookControllerDocs {

    @Operation(
        summary = "Find All Book",
        description = "Finds All Book",
        tags = ["Book"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = ArraySchema(schema = Schema(implementation = BookDTO::class))
                    )
                ]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun findAll(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String
    ): ResponseEntity<PagedModel<EntityModel<BookDTO>>>

    @Operation(
        summary = "Finds a Book",
        description = "Find a specific book by your ID",
        tags = ["Book"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = BookDTO::class))]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun findById(@PathVariable("id") id: Long): BookDTO

    @Operation(
        summary = "Adds a new Book",
        description = "Adds a new book by passing in a JSON, XML or YML representation of the book.",
        tags = ["Book"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = BookDTO::class))]
            ),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun create(@RequestBody book: BookDTO): BookDTO

    @Operation(
        summary = "Updates a book's information",
        description = "Updates a book's information by passing in a JSON, XML or YML representation of the updated book.",
        tags = ["Book"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = BookDTO::class))]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun update(@RequestBody book: BookDTO): BookDTO

    @Operation(
        summary = "Deletes a Book",
        description = "Deletes a specific book by their ID",
        tags = ["Book"],
        responses = [
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun delete(@PathVariable("id") id: Long): ResponseEntity<*>
}
