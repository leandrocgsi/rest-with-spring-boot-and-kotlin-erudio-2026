package br.com.erudio.controllers.docs

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.MediaTypes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

interface PersonControllerDocs {

    @Operation(
        summary = "Find All People",
        description = "Finds All People",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = ArraySchema(schema = Schema(implementation = PersonDTO::class))
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
    ): ResponseEntity<PagedModel<EntityModel<PersonDTO>>>

    @Operation(
        summary = "Export People",
        description = "Export a Page of People in XLSX and CSV format",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [
                    Content(mediaType = MediaTypes.APPLICATION_XLSX_VALUE),
                    Content(mediaType = MediaTypes.APPLICATION_CSV_VALUE)
                ]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun exportPage(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String,
        request: HttpServletRequest?
    ): ResponseEntity<Resource>

    @Operation(
        summary = "Massive People Creation",
        description = "Massive People Creation with upload of XLSX or CSV",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [
                    Content(schema = Schema(implementation = PersonDTO::class))
                ]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun massCreation(file: MultipartFile): List<PersonDTO>

    @Operation(
        summary = "Find People by FirstName",
        description = "Finds People by their First Names",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = ArraySchema(schema = Schema(implementation = PersonDTO::class))
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
    fun findByName(
        @PathVariable("firstName") firstName: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String
    ): ResponseEntity<PagedModel<EntityModel<PersonDTO>>>

    @Operation(
        summary = "Finds a Person",
        description = "Find a specific person by your ID",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PersonDTO::class))]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun findById(@PathVariable("id") id: Long): PersonDTO

    @Operation(
        summary = "Export Person data as PDF",
        description = "Export a specific Person data as PDF by your ID",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(mediaType = MediaTypes.APPLICATION_PDF_VALUE)]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun export(
        @PathVariable("id") id: Long,
        request: HttpServletRequest
    ): ResponseEntity<Resource>

    @Operation(
        summary = "Adds a new Person",
        description = "Adds a new person by passing in a JSON, XML or YML representation of the person.",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PersonDTO::class))]
            ),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun create(@RequestBody person: PersonDTO): PersonDTO

    @Operation(
        summary = "Updates a person's information",
        description = "Updates a person's information by passing in a JSON, XML or YML representation of the updated person.",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PersonDTO::class))]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun update(@RequestBody person: PersonDTO): PersonDTO

    @Operation(
        summary = "Disable a Person",
        description = "Disable a specific person by your ID",
        tags = ["People"],
        responses = [
            ApiResponse(
                description = "Success",
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PersonDTO::class))]
            ),
            ApiResponse(description = "No Content", responseCode = "204", content = [Content()]),
            ApiResponse(description = "Bad Request", responseCode = "400", content = [Content()]),
            ApiResponse(description = "Unauthorized", responseCode = "401", content = [Content()]),
            ApiResponse(description = "Not Found", responseCode = "404", content = [Content()]),
            ApiResponse(description = "Internal Server Error", responseCode = "500", content = [Content()])
        ]
    )
    fun disablePerson(@PathVariable("id") id: Long): PersonDTO

    @Operation(
        summary = "Deletes a Person",
        description = "Deletes a specific person by their ID",
        tags = ["People"],
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
