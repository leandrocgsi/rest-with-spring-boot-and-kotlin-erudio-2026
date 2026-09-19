package br.com.erudio.controllers

import br.com.erudio.controllers.docs.PersonControllerDocs
import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.services.PersonService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/person/v1")
@Tag(name = "People", description = "Endpoints for Managing People")
class PersonController : PersonControllerDocs {

    @Autowired
    private lateinit var service: PersonService

    @GetMapping(
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun findAll(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String
    ): ResponseEntity<PagedModel<EntityModel<PersonDTO>>> {
        val sortDirection = if ("desc".equals(direction, ignoreCase = true)) Direction.DESC else Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, "firstName"))
        return ResponseEntity.ok(service.findAll(pageable))
    }

    @GetMapping(
        value = ["/exportPage"],
        produces = [
            MediaTypes.APPLICATION_XLSX_VALUE,
            MediaTypes.APPLICATION_CSV_VALUE,
            MediaTypes.APPLICATION_PDF_VALUE]
    )
    override fun exportPage(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String,
        request: HttpServletRequest?
    ): ResponseEntity<Resource> {
        val sortDirection = if ("desc".equals(direction, ignoreCase = true)) Direction.DESC else Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, "firstName"))

        val acceptHeader: String? = request?.getHeader(HttpHeaders.ACCEPT)

        val file = service.exportPage(pageable, acceptHeader)

        val extensionMap = mapOf(
            MediaTypes.APPLICATION_XLSX_VALUE to ".xlsx",
            MediaTypes.APPLICATION_CSV_VALUE to ".csv",
            MediaTypes.APPLICATION_PDF_VALUE to ".pdf"
        )

        val fileExtension = extensionMap[acceptHeader] ?: ""
        val contentType = acceptHeader ?: "application/octet-stream"

        val filename = "people_exported$fileExtension"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$filename\""
            )
            .body(file)
    }

    @GetMapping(
        value = ["/findPeopleByName/{firstName}"],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun findByName(
        @PathVariable("firstName") firstName: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "12") size: Int,
        @RequestParam(value = "direction", defaultValue = "asc") direction: String
    ): ResponseEntity<PagedModel<EntityModel<PersonDTO>>> {
        val sortDirection = if ("desc".equals(direction, ignoreCase = true)) Direction.DESC else Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, "firstName"))
        return ResponseEntity.ok(service.findByName(firstName, pageable))
    }

    @GetMapping(
        value = ["/{id}"],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun findById(@PathVariable("id") id: Long): PersonDTO {
        return service.findById(id)
    }

    @GetMapping(
        value = ["/export/{id}"],
        produces = [MediaTypes.APPLICATION_PDF_VALUE]
    )
    override fun export(@PathVariable("id") id: Long, request: HttpServletRequest): ResponseEntity<Resource> {

        val acceptHeader = request.getHeader(HttpHeaders.ACCEPT)
        val file = service.exportPerson(id, acceptHeader)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(acceptHeader))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=person.pdf"
            )
            .body(file)
    }

    @PostMapping(
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun create(@RequestBody person: PersonDTO): PersonDTO {
        return service.create(person)
    }

    @PostMapping(
        value = ["/massCreation"],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun massCreation(@RequestParam("file") file: MultipartFile): List<PersonDTO> {
        return service.massCreation(file)
    }

    @PutMapping(
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun update(@RequestBody person: PersonDTO): PersonDTO {
        return service.update(person)
    }

    @PatchMapping(
        value = ["/{id}"],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun disablePerson(@PathVariable("id") id: Long): PersonDTO {
        return service.disablePerson(id)
    }

    @DeleteMapping(value = ["/{id}"])
    override fun delete(@PathVariable("id") id: Long): ResponseEntity<*> {
        service.delete(id)
        return ResponseEntity.noContent().build<Any>()
    }
}
