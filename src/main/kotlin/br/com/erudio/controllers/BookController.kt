package br.com.erudio.controllers

import br.com.erudio.controllers.docs.BookControllerDocs
import br.com.erudio.data.dto.BookDTO
import br.com.erudio.services.BookService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/book/v1")
@Tag(name = "Book", description = "Endpoints for Managing Book")
class BookController : BookControllerDocs {

    @Autowired
    private lateinit var service: BookService

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
    ): ResponseEntity<PagedModel<EntityModel<BookDTO>>> {
        val sortDirection = if ("desc".equals(direction, ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, "title"))
        return ResponseEntity.ok(service.findAll(pageable))
    }

    @GetMapping(
        value = ["/{id}"],
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_YAML_VALUE]
    )
    override fun findById(@PathVariable("id") id: Long): BookDTO {
        return service.findById(id)
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
    override fun create(@RequestBody book: BookDTO): BookDTO {
        return service.create(book)
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
    override fun update(@RequestBody book: BookDTO): BookDTO {
        return service.update(book)
    }

    @DeleteMapping(value = ["/{id}"])
    override fun delete(@PathVariable("id") id: Long): ResponseEntity<*> {
        service.delete(id)
        return ResponseEntity.noContent().build<Any>()
    }
}
