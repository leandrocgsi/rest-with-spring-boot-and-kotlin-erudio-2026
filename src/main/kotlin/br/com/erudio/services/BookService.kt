package br.com.erudio.services

import br.com.erudio.controllers.BookController
import br.com.erudio.data.dto.BookDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.mapper.DozerMapper.parseObject
import br.com.erudio.model.Book
import br.com.erudio.repository.BookRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class BookService {

    private val logger = Logger.getLogger(BookService::class.java.name)

    @Autowired
    private lateinit var repository: BookRepository

    @Autowired
    private lateinit var assembler: PagedResourcesAssembler<BookDTO>

    fun findAll(pageable: Pageable): PagedModel<EntityModel<BookDTO>> {

        logger.info("Finding all Book!")

        val books = repository.findAll(pageable)

        val booksWithLinks = books.map { book ->
            val dto = parseObject(book, BookDTO::class.java)
            addHateoasLinks(dto)
            dto
        }

        val findAllLink = linkTo(
            methodOn(BookController::class.java)
                .findAll(
                    pageable.pageNumber,
                    pageable.pageSize,
                    pageable.sort.toString()
                )
        ).withSelfRel()
        return assembler.toModel(booksWithLinks, findAllLink)
    }

    fun findById(id: Long): BookDTO {
        logger.info("Finding one Book!")

        val entity = repository.findById(id)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }
        val dto = parseObject(entity, BookDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun create(book: BookDTO?): BookDTO {

        if (book == null) throw RequiredObjectIsNullException()

        logger.info("Creating one Book!")
        val entity = parseObject(book, Book::class.java)

        val dto = parseObject(repository.save(entity), BookDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun update(book: BookDTO?): BookDTO {

        if (book == null) throw RequiredObjectIsNullException()

        logger.info("Updating one Book!")
        val entity = repository.findById(book.id!!)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }

        entity.author = book.author
        entity.launchDate = book.launchDate
        entity.price = book.price
        entity.title = book.title

        val dto = parseObject(repository.save(entity), BookDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun delete(id: Long) {

        logger.info("Deleting one Book!")

        val entity = repository.findById(id)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }
        repository.delete(entity)
    }

    private fun addHateoasLinks(dto: BookDTO) {
        dto.add(linkTo(methodOn(BookController::class.java).findById(dto.id!!)).withSelfRel().withType("GET"))
        dto.add(linkTo(methodOn(BookController::class.java).findAll(1, 12, "asc")).withRel("findAll").withType("GET"))
        dto.add(linkTo(methodOn(BookController::class.java).create(dto)).withRel("create").withType("POST"))
        dto.add(linkTo(methodOn(BookController::class.java).update(dto)).withRel("update").withType("PUT"))
        dto.add(linkTo(methodOn(BookController::class.java).delete(dto.id!!)).withRel("delete").withType("DELETE"))
    }
}
