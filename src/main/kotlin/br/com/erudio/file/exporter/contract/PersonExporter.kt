package br.com.erudio.file.exporter.contract

import br.com.erudio.data.dto.PersonDTO
import org.springframework.core.io.Resource

interface PersonExporter {

    @Throws(Exception::class)
    fun exportPeople(people: List<PersonDTO>): Resource

    @Throws(Exception::class)
    fun exportPerson(person: PersonDTO): Resource?
}
