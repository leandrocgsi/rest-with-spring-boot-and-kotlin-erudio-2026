package br.com.erudio.integrationtests.controllers.file

import br.com.erudio.integrationtests.AuthenticatedIntegrationTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FileControllerTest : AuthenticatedIntegrationTest() {

    companion object {
        private const val BASE = "/api/file/v1"

        private fun uniqueName(extension: String): String {
            return UUID.randomUUID().toString() + extension
        }

        private fun upload(name: String, content: ByteArray, contentType: String): ByteArray {
            given().spec(authenticated())
                .multiPart("file", name, content, contentType)
                .`when`()
                .post("$BASE/uploadFile")
                .then()
                .statusCode(200)
            return content
        }
    }

    @Test
    fun uploadStoresTheFileAndDescribesIt() {
        val name = uniqueName(".txt")
        val content = "hello upload".toByteArray(Charsets.UTF_8)

        given().spec(authenticated())
            .multiPart("file", name, content, "text/plain")
            .`when`()
            .post("$BASE/uploadFile")
            .then()
            .statusCode(200)
            .body("fileName", equalTo(name))
            .body("fileType", equalTo("text/plain"))
            .body("size", equalTo(content.size))
            .body("fileDownloadUri", endsWith("$BASE/downloadFile/$name"))
    }

    @Test
    fun uploadedFileCanBeDownloadedWithTheSameContent() {
        val name = uniqueName(".txt")
        val content = upload(name, "Formação Spring Boot 2026".toByteArray(Charsets.UTF_8), "text/plain")

        val downloaded = given().spec(authenticated())
            .`when`()
            .get("$BASE/downloadFile/$name")
            .then()
            .statusCode(200)
            .header("Content-Disposition", equalTo("attachment; filename=\"$name\""))
            .contentType(startsWith("text/plain"))
            .extract()
            .asByteArray()

        assertArrayEquals(content, downloaded)
    }

    @Test
    fun binaryFilesSurviveTheRoundTripByteForByte() {
        val name = uniqueName(".bin")
        val content = ByteArray(2 * 1024 * 1024)
        Random(42).nextBytes(content)
        upload(name, content, "application/octet-stream")

        val downloaded = given().spec(authenticated())
            .`when`()
            .get("$BASE/downloadFile/$name")
            .then()
            .statusCode(200)
            .contentType("application/octet-stream")
            .extract()
            .asByteArray()

        assertArrayEquals(content, downloaded)
    }

    @Test
    fun theContentTypeOfTheDownloadFollowsTheFileExtension() {
        val csv = uniqueName(".csv")
        upload(csv, "a,b\n1,2\n".toByteArray(Charsets.UTF_8), "text/csv")

        given().spec(authenticated())
            .`when`()
            .get("$BASE/downloadFile/$csv")
            .then()
            .statusCode(200)
            .contentType(startsWith("text/csv"))
    }

    @Test
    fun uploadingAnExistingNameReplacesTheFile() {
        val name = uniqueName(".txt")
        upload(name, "first version".toByteArray(Charsets.UTF_8), "text/plain")
        val second = upload(name, "second version".toByteArray(Charsets.UTF_8), "text/plain")

        val downloaded = given().spec(authenticated())
            .`when`()
            .get("$BASE/downloadFile/$name")
            .then()
            .statusCode(200)
            .extract()
            .asByteArray()

        assertArrayEquals(second, downloaded)
    }

    @Test
    fun uploadMultipleFilesStoresAllOfThemAndReturnsOneDescriptionEach() {
        val first = uniqueName(".txt")
        val second = uniqueName(".csv")

        given().spec(authenticated())
            .multiPart("files", first, "one".toByteArray(Charsets.UTF_8), "text/plain")
            .multiPart("files", second, "a,b\n".toByteArray(Charsets.UTF_8), "text/csv")
            .`when`()
            .post("$BASE/uploadMultipleFiles")
            .then()
            .statusCode(200)
            .body("size()", `is`(2))
            .body("fileName", contains(first, second))
            .body("fileType", contains("text/plain", "text/csv"))
            .body(
                "fileDownloadUri", contains(
                    endsWith("$BASE/downloadFile/$first"),
                    endsWith("$BASE/downloadFile/$second")
                )
            )

        for (name in arrayOf(first, second)) {
            given().spec(authenticated()).`when`().get("$BASE/downloadFile/$name").then().statusCode(200)
        }
    }

    @Test
    fun downloadingAFileThatDoesNotExistIsNotFound() {
        val name = uniqueName(".txt")

        given().spec(authenticated())
            .`when`()
            .get("$BASE/downloadFile/$name")
            .then()
            .statusCode(404)
            .body("message", equalTo("File not found $name"))
    }

    @Test
    fun aFileNameThatEscapesTheUploadDirectoryIsRejectedAndNothingIsWritten() {
        val tag = UUID.randomUUID().toString()
        val name = "../escaped-$tag.txt"

        given().spec(authenticated())
            .multiPart("file", name, "boom".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post("$BASE/uploadFile")
            .then()
            .statusCode(500)
            .body("message", equalTo("Could not store file $name. Please try Again!"))

        assertFalse(Files.exists(Path.of("target", "escaped-$tag.txt")))
    }

    @Test
    fun uploadWithoutTheFilePartIsABadRequest() {
        given().spec(authenticated())
            .multiPart("somethingElse", uniqueName(".txt"), "x".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post("$BASE/uploadFile")
            .then()
            .statusCode(400)
    }

    @Test
    fun uploadAndDownloadRequireAuthentication() {
        given().spec(anonymous())
            .multiPart("file", uniqueName(".txt"), "x".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post("$BASE/uploadFile")
            .then()
            .statusCode(403)

        given().spec(anonymous())
            .multiPart("files", uniqueName(".txt"), "x".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post("$BASE/uploadMultipleFiles")
            .then()
            .statusCode(403)

        given().spec(anonymous())
            .`when`()
            .get("$BASE/downloadFile/whatever.txt")
            .then()
            .statusCode(403)
    }
}
