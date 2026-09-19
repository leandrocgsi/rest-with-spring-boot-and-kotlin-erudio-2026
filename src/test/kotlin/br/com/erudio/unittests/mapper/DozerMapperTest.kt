package br.com.erudio.unittests.mapper

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.mapper.DozerMapper.parseListObjects
import br.com.erudio.mapper.DozerMapper.parseObject
import br.com.erudio.model.Person
import br.com.erudio.unittests.mapper.mocks.MockPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DozerMapperTest {

    private lateinit var inputObject: MockPerson

    @BeforeEach
    fun setUp() {
        inputObject = MockPerson()
    }

    @Test
    fun parseEntityToDTOTest() {
        val output = parseObject(inputObject.mockEntity(), PersonDTO::class.java)
        assertEquals(0L, output.id)
        assertEquals("First Name Test0", output.firstName)
        assertEquals("Last Name Test0", output.lastName)
        assertEquals("Address Test0", output.address)
        assertEquals("Male", output.gender)
    }

    @Test
    fun parseEntityListToDTOListTest() {
        val outputList = parseListObjects(inputObject.mockEntityList(), PersonDTO::class.java)
        val outputZero = outputList[0]

        assertEquals(0L, outputZero.id)
        assertEquals("First Name Test0", outputZero.firstName)
        assertEquals("Last Name Test0", outputZero.lastName)
        assertEquals("Address Test0", outputZero.address)
        assertEquals("Male", outputZero.gender)

        val outputSeven = outputList[7]

        assertEquals(7L, outputSeven.id)
        assertEquals("First Name Test7", outputSeven.firstName)
        assertEquals("Last Name Test7", outputSeven.lastName)
        assertEquals("Address Test7", outputSeven.address)
        assertEquals("Female", outputSeven.gender)

        val outputTwelve = outputList[12]

        assertEquals(12L, outputTwelve.id)
        assertEquals("First Name Test12", outputTwelve.firstName)
        assertEquals("Last Name Test12", outputTwelve.lastName)
        assertEquals("Address Test12", outputTwelve.address)
        assertEquals("Male", outputTwelve.gender)
    }

    @Test
    fun parseDTOToEntityTest() {
        val output = parseObject(inputObject.mockDTO(), Person::class.java)
        assertEquals(0L, output.id)
        assertEquals("First Name Test0", output.firstName)
        assertEquals("Last Name Test0", output.lastName)
        assertEquals("Address Test0", output.address)
        assertEquals("Male", output.gender)
    }

    @Test
    fun parserDTOListToEntityListTest() {
        val outputList = parseListObjects(inputObject.mockDTOList(), Person::class.java)
        val outputZero = outputList[0]

        assertEquals(0L, outputZero.id)
        assertEquals("First Name Test0", outputZero.firstName)
        assertEquals("Last Name Test0", outputZero.lastName)
        assertEquals("Address Test0", outputZero.address)
        assertEquals("Male", outputZero.gender)

        val outputSeven = outputList[7]

        assertEquals(7L, outputSeven.id)
        assertEquals("First Name Test7", outputSeven.firstName)
        assertEquals("Last Name Test7", outputSeven.lastName)
        assertEquals("Address Test7", outputSeven.address)
        assertEquals("Female", outputSeven.gender)

        val outputTwelve = outputList[12]

        assertEquals(12L, outputTwelve.id)
        assertEquals("First Name Test12", outputTwelve.firstName)
        assertEquals("Last Name Test12", outputTwelve.lastName)
        assertEquals("Address Test12", outputTwelve.address)
        assertEquals("Male", outputTwelve.gender)
    }
}
