package br.com.erudio.unittests.file

import br.com.erudio.file.importer.impl.CsvImporter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class CsvImporterTest {

    private val importer = CsvImporter()

    private fun csv(content: String): InputStream {
        return ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun importsOnePersonPerLine() {
        val people = importer.importFile(
            csv(
                """
                first_name,last_name,address,gender
                Ada,Lovelace,London,Female
                Alan,Turing,Wilmslow,Male
                """.trimIndent()
            )
        )

        assertEquals(2, people.size)
        val ada = people[0]
        assertEquals("Ada", ada.firstName)
        assertEquals("Lovelace", ada.lastName)
        assertEquals("London", ada.address)
        assertEquals("Female", ada.gender)
        assertEquals("Alan", people[1].firstName)
    }

    @Test
    fun importedPeopleAreEnabledAndHaveNoIdYet() {
        val people = importer.importFile(
            csv(
                """
                first_name,last_name,address,gender
                Ada,Lovelace,London,Female
                """.trimIndent()
            )
        )

        assertTrue(people[0].enabled!!)
        assertNull(people[0].id)
    }

    @Test
    fun theOrderOfTheColumnsDoesNotMatter() {
        val people = importer.importFile(
            csv(
                """
                gender,address,last_name,first_name
                Female,London,Lovelace,Ada
                """.trimIndent()
            )
        )

        assertEquals("Ada", people[0].firstName)
        assertEquals("Lovelace", people[0].lastName)
        assertEquals("London", people[0].address)
        assertEquals("Female", people[0].gender)
    }

    @Test
    fun trimsSpacesAroundTheValues() {
        val people = importer.importFile(csv("first_name,last_name,address,gender\n  Ada  ,  Lovelace ,  London  , Female \n"))

        assertEquals("Ada", people[0].firstName)
        assertEquals("Lovelace", people[0].lastName)
        assertEquals("London", people[0].address)
        assertEquals("Female", people[0].gender)
    }

    @Test
    fun ignoresEmptyLines() {
        val people = importer.importFile(csv("first_name,last_name,address,gender\n\nAda,Lovelace,London,Female\n\n\nAlan,Turing,Wilmslow,Male\n\n"))

        assertEquals(2, people.size)
    }

    @Test
    fun keepsCommasInsideQuotedValues() {
        val people = importer.importFile(csv("first_name,last_name,address,gender\nAda,Lovelace,\"Rua A, 10 - apto 2\",Female\n"))

        assertEquals("Rua A, 10 - apto 2", people[0].address)
    }

    @Test
    fun readsUtf8Accents() {
        val people = importer.importFile(csv("first_name,last_name,address,gender\nJoão,Conceição,Avenida São João,Male\n"))

        assertEquals("João", people[0].firstName)
        assertEquals("Conceição", people[0].lastName)
        assertEquals("Avenida São João", people[0].address)
    }

    @Test
    fun importsNothingFromAHeaderOnlyFile() {
        assertTrue(importer.importFile(csv("first_name,last_name,address,gender\n")).isEmpty())
    }

    @Test
    fun importsNothingFromAnEmptyFile() {
        assertTrue(importer.importFile(csv("")).isEmpty())
    }

    @Test
    fun failsWhenARequiredColumnIsMissing() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            importer.importFile(csv("first_name,last_name,address\nAda,Lovelace,London\n"))
        }

        assertTrue(exception.message!!.contains("gender"), exception.message)
    }

    @Test
    fun failsWhenALineHasFewerValuesThanTheHeader() {
        assertThrows(IllegalArgumentException::class.java) {
            importer.importFile(csv("first_name,last_name,address,gender\nAda,Lovelace\n"))
        }
    }
}
