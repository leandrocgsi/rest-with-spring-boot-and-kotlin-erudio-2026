package br.com.erudio.unittests.services

import br.com.erudio.config.FileStorageConfig
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.services.FileStorageService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class FileStorageServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var uploadDir: Path
    private lateinit var service: FileStorageService

    @BeforeEach
    fun setUp() {
        uploadDir = tempDir.resolve("uploads")
        service = FileStorageService(configFor(uploadDir))
    }

    private fun configFor(directory: Path): FileStorageConfig {
        val config = FileStorageConfig()
        config.uploadDir = directory.toString()
        return config
    }

    private fun file(name: String, content: String): MultipartFile {
        return MockMultipartFile("file", name, "text/plain", content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun createsTheUploadDirectoryWhenItDoesNotExist() {
        val nested = tempDir.resolve("a").resolve("b").resolve("uploads")

        FileStorageService(configFor(nested))

        assertTrue(Files.isDirectory(nested))
    }

    @Test
    fun failsWhenTheUploadDirectoryCannotBeCreated() {
        val regularFile = Files.writeString(tempDir.resolve("not-a-directory"), "x")

        val exception = assertThrows(FileStorageException::class.java) {
            FileStorageService(configFor(regularFile.resolve("uploads")))
        }

        assertEquals("Could not create the directory where files will be stored!", exception.message)
    }

    @Test
    fun storesTheFileWithItsContentAndReturnsItsName() {
        val stored = service.storeFile(file("notes.txt", "hello upload"))

        assertEquals("notes.txt", stored)
        assertEquals("hello upload", Files.readString(uploadDir.resolve("notes.txt")))
    }

    @Test
    fun replacesAFileThatAlreadyExists() {
        service.storeFile(file("notes.txt", "first"))
        service.storeFile(file("notes.txt", "second"))

        assertEquals("second", Files.readString(uploadDir.resolve("notes.txt")))
    }

    @Test
    fun cleansRedundantPathSegmentsFromTheName() {
        val stored = service.storeFile(file("folder/../notes.txt", "content"))

        assertEquals("notes.txt", stored)
        assertTrue(Files.exists(uploadDir.resolve("notes.txt")))
    }

    @Test
    fun rejectsANameThatEscapesTheUploadDirectory() {
        val exception = assertThrows(FileStorageException::class.java) {
            service.storeFile(file("../evil.txt", "boom"))
        }

        assertEquals("Could not store file ../evil.txt. Please try Again!", exception.message)
        assertInstanceOf(FileStorageException::class.java, exception.cause)
        assertTrue(exception.cause!!.message!!.contains("Invalid path Sequence"))
        assertFalse(Files.exists(tempDir.resolve("evil.txt")))
    }

    @Test
    fun reportsAFailureReadingTheUploadedContent() {
        val broken = mock<MultipartFile>()
        whenever(broken.originalFilename).thenReturn("broken.txt")
        whenever(broken.inputStream).thenThrow(IOException("connection reset"))

        val exception = assertThrows(FileStorageException::class.java) { service.storeFile(broken) }

        assertEquals("Could not store file broken.txt. Please try Again!", exception.message)
        assertInstanceOf(IOException::class.java, exception.cause)
    }

    @Test
    fun loadsAFileThatWasStored() {
        service.storeFile(file("notes.txt", "stored content"))

        val resource = service.loadFileAsResource("notes.txt")

        assertTrue(resource.exists())
        assertEquals("notes.txt", resource.filename)
        assertEquals("stored content", String(resource.contentAsByteArray, Charsets.UTF_8))
    }

    @Test
    fun failsToLoadAFileThatDoesNotExist() {
        val exception = assertThrows(FileNotFoundException::class.java) {
            service.loadFileAsResource("missing.txt")
        }

        assertEquals("File not found missing.txt", exception.message)
    }
}
