package br.com.erudio.unittests.mapper.mocks

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.model.Person

class MockPerson {

    fun mockEntity(): Person {
        return mockEntity(0)
    }

    fun mockDTO(): PersonDTO {
        return mockDTO(0)
    }

    fun mockEntityList(): List<Person> {
        val persons: MutableList<Person> = ArrayList()
        for (i in 0..13) {
            persons.add(mockEntity(i))
        }
        return persons
    }

    fun mockDTOList(): List<PersonDTO> {
        val persons: MutableList<PersonDTO> = ArrayList()
        for (i in 0..13) {
            persons.add(mockDTO(i))
        }
        return persons
    }

    fun mockEntity(number: Int): Person {
        val person = Person()
        person.address = "Address Test$number"
        person.firstName = "First Name Test$number"
        person.gender = if (number % 2 == 0) "Male" else "Female"
        person.id = number.toLong()
        person.lastName = "Last Name Test$number"
        return person
    }

    fun mockDTO(number: Int): PersonDTO {
        val person = PersonDTO()
        person.address = "Address Test$number"
        person.firstName = "First Name Test$number"
        person.gender = if (number % 2 == 0) "Male" else "Female"
        person.id = number.toLong()
        person.lastName = "Last Name Test$number"
        return person
    }
}
