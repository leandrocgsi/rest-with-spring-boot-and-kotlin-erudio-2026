package br.com.erudio.unittests.services

import br.com.erudio.config.AwsS3Properties
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.services.FileStorageService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.IOException

class FileStorageServiceTest {

    companion object {
        private const val BUCKET = "erudio-files-test"
    }

    private lateinit var s3Client: S3Client
    private lateinit var service: FileStorageService

    @BeforeEach
    fun setUp() {
        s3Client = mock()
        whenever(s3Client.headBucket(any<HeadBucketRequest>())).thenReturn(HeadBucketResponse.builder().build())
        service = FileStorageService(s3Client, propertiesFor(BUCKET))
    }

    private fun propertiesFor(bucket: String): AwsS3Properties {
        val properties = AwsS3Properties()
        properties.bucket = bucket
        return properties
    }

    private fun file(name: String, content: String): MultipartFile {
        return MockMultipartFile("file", name, "text/plain", content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun doesNotRecreateTheBucketWhenItAlreadyExists() {
        verify(s3Client).headBucket(any<HeadBucketRequest>())
        verify(s3Client, never()).createBucket(any<CreateBucketRequest>())
    }

    @Test
    fun createsTheBucketWhenItDoesNotExist() {
        val freshClient: S3Client = mock()
        whenever(freshClient.headBucket(any<HeadBucketRequest>()))
            .thenThrow(NoSuchBucketException.builder().message("missing").build())

        FileStorageService(freshClient, propertiesFor(BUCKET))

        verify(freshClient).createBucket(argThat<CreateBucketRequest> { bucket() == BUCKET })
    }

    @Test
    fun failsWhenTheBucketCannotBeVerifiedOrCreated() {
        val brokenClient: S3Client = mock()
        whenever(brokenClient.headBucket(any<HeadBucketRequest>()))
            .thenThrow(NoSuchBucketException.builder().message("missing").build())
        whenever(brokenClient.createBucket(any<CreateBucketRequest>()))
            .thenThrow(S3Exception.builder().message("boom").build())

        val exception = assertThrows(FileStorageException::class.java) {
            FileStorageService(brokenClient, propertiesFor(BUCKET))
        }

        assertEquals("Could not verify or create the S3 bucket where files will be stored!", exception.message)
    }

    @Test
    fun storesTheFileWithItsContentAndReturnsItsName() {
        val stored = service.storeFile(file("notes.txt", "hello upload"))

        assertEquals("notes.txt", stored)

        val requestCaptor = argumentCaptor<PutObjectRequest>()
        val bodyCaptor = argumentCaptor<RequestBody>()
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture())

        assertEquals(BUCKET, requestCaptor.firstValue.bucket())
        assertEquals("notes.txt", requestCaptor.firstValue.key())
        assertEquals("hello upload", contentOf(bodyCaptor.firstValue))
    }

    @Test
    fun replacesAFileThatAlreadyExists() {
        service.storeFile(file("notes.txt", "first"))
        service.storeFile(file("notes.txt", "second"))

        val bodyCaptor = argumentCaptor<RequestBody>()
        verify(s3Client, times(2)).putObject(any<PutObjectRequest>(), bodyCaptor.capture())

        assertEquals("second", contentOf(bodyCaptor.secondValue))
    }

    @Test
    fun cleansRedundantPathSegmentsFromTheName() {
        val stored = service.storeFile(file("folder/../notes.txt", "content"))

        assertEquals("notes.txt", stored)
        verify(s3Client).putObject(argThat<PutObjectRequest> { key() == "notes.txt" }, any<RequestBody>())
    }

    @Test
    fun rejectsANameThatEscapesTheUploadDirectory() {
        val exception = assertThrows(FileStorageException::class.java) {
            service.storeFile(file("../evil.txt", "boom"))
        }

        assertEquals("Could not store file ../evil.txt. Please try Again!", exception.message)
        assertInstanceOf(FileStorageException::class.java, exception.cause)
        assertTrue(exception.cause!!.message!!.contains("Invalid path Sequence"))
        verify(s3Client, never()).putObject(any<PutObjectRequest>(), any<RequestBody>())
    }

    @Test
    fun reportsAFailureReadingTheUploadedContent() {
        val broken: MultipartFile = mock()
        whenever(broken.originalFilename).thenReturn("broken.txt")
        whenever(broken.bytes).thenThrow(IOException("connection reset"))

        val exception = assertThrows(FileStorageException::class.java) { service.storeFile(broken) }

        assertEquals("Could not store file broken.txt. Please try Again!", exception.message)
        assertInstanceOf(IOException::class.java, exception.cause)
    }

    @Test
    fun loadsAFileThatWasStored() {
        whenever(s3Client.getObjectAsBytes(argThat<GetObjectRequest> { key() == "notes.txt" }))
            .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "stored content".toByteArray(Charsets.UTF_8)))

        val resource = service.loadFileAsResource("notes.txt")

        assertTrue(resource.exists())
        assertEquals("notes.txt", resource.filename)
        assertEquals("stored content", String(resource.contentAsByteArray, Charsets.UTF_8))
    }

    @Test
    fun failsToLoadAFileThatDoesNotExist() {
        whenever(s3Client.getObjectAsBytes(any<GetObjectRequest>()))
            .thenThrow(NoSuchKeyException.builder().message("missing").build())

        val exception = assertThrows(FileNotFoundException::class.java) {
            service.loadFileAsResource("missing.txt")
        }

        assertEquals("File not found missing.txt", exception.message)
    }

    private fun contentOf(body: RequestBody): String {
        return String(body.contentStreamProvider().newStream().readAllBytes(), Charsets.UTF_8)
    }
}
