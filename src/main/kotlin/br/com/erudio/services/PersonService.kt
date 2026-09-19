package br.com.erudio.services

import br.com.erudio.controllers.PersonController
import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.BadRequestException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.mapper.DozerMapper.parseObject
import br.com.erudio.model.Person
import br.com.erudio.repository.PersonRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.logging.Logger

@Service
class PersonService {

    private val logger = Logger.getLogger(PersonService::class.java.name)

    @Autowired
    private lateinit var repository: PersonRepository

    @Autowired
    private lateinit var importer: FileImporterFactory

    @Autowired
    private lateinit var exporter: FileExporterFactory

    @Autowired
    private lateinit var assembler: PagedResourcesAssembler<PersonDTO>

    fun findAll(pageable: Pageable): PagedModel<EntityModel<PersonDTO>> {

        logger.info("Finding all People!")

        val people = repository.findAll(pageable)
        return buildPagedModel(pageable, people)
    }

    fun findByName(firstName: String, pageable: Pageable): PagedModel<EntityModel<PersonDTO>> {

        logger.info("Finding People by name!")

        val people = repository.findPeopleByName(firstName, pageable)
        return buildPagedModel(pageable, people)
    }

    fun exportPage(pageable: Pageable, acceptHeader: String?): Resource {

        logger.info("Exporting a People page!")

        val people = repository.findAll(pageable)
            .map { person -> parseObject(person, PersonDTO::class.java) }
            .content

        try {
            val personExporter = exporter.getExporter(acceptHeader)
            return personExporter.exportPeople(people)
        } catch (e: Exception) {
            throw RuntimeException("Error during file export!", e)
        }
    }

    fun exportPerson(id: Long, acceptHeader: String?): Resource? {
        logger.info("Exporting data of one Person!")

        val person = repository.findById(id)
            .map { entity -> parseObject(entity, PersonDTO::class.java) }
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }

        try {
            val personExporter = exporter.getExporter(acceptHeader)
            return personExporter.exportPerson(person)
        } catch (e: Exception) {
            throw RuntimeException("Error during file export!", e)
        }
    }

    fun findById(id: Long): PersonDTO {
        logger.info("Finding one Person!")

        val entity = repository.findById(id)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }
        val dto = parseObject(entity, PersonDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun create(person: PersonDTO?): PersonDTO {

        if (person == null) throw RequiredObjectIsNullException()

        logger.info("Creating one Person!")
        val entity = parseObject(person, Person::class.java)

        val dto = parseObject(repository.save(entity), PersonDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun massCreation(file: MultipartFile): List<PersonDTO> {
        logger.info("Importing People from file!")

        if (file.isEmpty) throw BadRequestException("Please set a Valid File!")

        try {
            file.inputStream.use { inputStream ->
                val filename = file.originalFilename
                    ?: throw BadRequestException("File name cannot be null")
                val fileImporter = importer.getImporter(filename)

                val entities = fileImporter.importFile(inputStream)
                    .map { dto -> repository.save(parseObject(dto, Person::class.java)) }

                return entities
                    .map { entity ->
                        val dto = parseObject(entity, PersonDTO::class.java)
                        addHateoasLinks(dto)
                        dto
                    }
            }
        } catch (e: Exception) {
            throw FileStorageException("Error processing the file!")
        }
    }

    fun update(person: PersonDTO?): PersonDTO {

        if (person == null) throw RequiredObjectIsNullException()

        logger.info("Updating one Person!")
        val entity = repository.findById(person.id!!)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }

        entity.firstName = person.firstName
        entity.lastName = person.lastName
        entity.address = person.address
        entity.gender = person.gender

        val dto = parseObject(repository.save(entity), PersonDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    @Transactional
    fun disablePerson(id: Long): PersonDTO {

        logger.info("Disabling one Person!")

        repository.findById(id)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }
        repository.disablePerson(id)

        val entity = repository.findById(id).get()
        val dto = parseObject(entity, PersonDTO::class.java)
        addHateoasLinks(dto)
        return dto
    }

    fun delete(id: Long) {

        logger.info("Deleting one Person!")

        val entity = repository.findById(id)
            .orElseThrow { ResourceNotFoundException("No records found for this ID!") }
        repository.delete(entity)
    }

    private fun buildPagedModel(pageable: Pageable, people: Page<Person>): PagedModel<EntityModel<PersonDTO>> {

        val peopleWithLinks = people.map { person ->
            val dto = parseObject(person, PersonDTO::class.java)
            addHateoasLinks(dto)
            dto
        }

        val findAllLink = linkTo(
            methodOn(PersonController::class.java)
                .findAll(
                    pageable.pageNumber,
                    pageable.pageSize,
                    pageable.sort.toString()
                )
        ).withSelfRel()
        return assembler.toModel(peopleWithLinks, findAllLink)
    }

    private fun addHateoasLinks(dto: PersonDTO) {
        dto.add(linkTo(methodOn(PersonController::class.java).findAll(1, 12, "asc")).withRel("findAll").withType("GET"))
        dto.add(linkTo(methodOn(PersonController::class.java).findByName("", 1, 12, "asc")).withRel("findByName").withType("GET"))
        dto.add(linkTo(methodOn(PersonController::class.java).findById(dto.id!!)).withSelfRel().withType("GET"))
        dto.add(linkTo(methodOn(PersonController::class.java).create(dto)).withRel("create").withType("POST"))
        dto.add(linkTo(methodOn(PersonController::class.java)).slash("massCreation").withRel("massCreation").withType("POST"))
        dto.add(linkTo(methodOn(PersonController::class.java).update(dto)).withRel("update").withType("PUT"))
        dto.add(linkTo(methodOn(PersonController::class.java).disablePerson(dto.id!!)).withRel("disable").withType("PATCH"))
        dto.add(linkTo(methodOn(PersonController::class.java).delete(dto.id!!)).withRel("delete").withType("DELETE"))

        dto.add(
            linkTo(
                methodOn(PersonController::class.java)
                    .exportPage(1, 12, "asc", null)
            )
                .withRel("exportPage")
                .withType("GET")
                .withTitle("Export People")
        )
    }
}
