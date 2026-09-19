package br.com.erudio.unittests.services

import br.com.erudio.config.EmailConfig
import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailSender
import br.com.erudio.services.EmailService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.RETURNS_SELF
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {

    companion object {
        private const val DEFAULT_SUBJECT = "Default Subject"
        private const val DEFAULT_MESSAGE = "Default Message"
    }

    private lateinit var emailSender: EmailSender
    private lateinit var emailConfig: EmailConfig
    private lateinit var service: EmailService

    @BeforeEach
    fun setUp() {
        emailSender = mock(defaultAnswer = RETURNS_SELF)
        emailConfig = EmailConfig()

        val defaults = EmailDefaultsConfig()
        defaults.subject = DEFAULT_SUBJECT
        defaults.message = DEFAULT_MESSAGE

        service = EmailService()
        ReflectionTestUtils.setField(service, "emailSender", emailSender)
        ReflectionTestUtils.setField(service, "emailConfigs", emailConfig)
        ReflectionTestUtils.setField(service, "emailDefaults", defaults)
    }

    private fun request(to: String?, subject: String?, body: String?): EmailRequestDTO {
        val request = EmailRequestDTO()
        request.to = to
        request.subject = subject
        request.body = body
        return request
    }

    private fun attachment(name: String, content: String): MultipartFile {
        return MockMultipartFile("attachment", name, "text/plain", content.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun sendSimpleEmailUsesTheSubjectAndBodyOfTheRequest() {
        service.sendSimpleEmail(request("ada@erudio.test", "Welcome", "<p>Hello Ada</p>"))

        verify(emailSender).to("ada@erudio.test")
        verify(emailSender).withSubject("Welcome")
        verify(emailSender).withMessage("<p>Hello Ada</p>")
        verify(emailSender).send(emailConfig)
        verify(emailSender, never()).attach(any())
        verify(emailSender, never()).attach(any(), anyOrNull())
    }

    @Test
    fun sendSimpleEmailSendsTheBodyAsTheMessageNotTheSubject() {
        service.sendSimpleEmail(request("ada@erudio.test", "Just the subject", "The real body"))

        verify(emailSender).withSubject("Just the subject")
        verify(emailSender, never()).withSubject("The real body")
        verify(emailSender).withMessage("The real body")
        verify(emailSender, never()).withMessage("Just the subject")
    }

    @Test
    fun sendSimpleEmailKeepsAnySubjectAndBodyTheCallerSets() {
        service.sendSimpleEmail(request("ada@erudio.test", "$DEFAULT_SUBJECT (custom)", "$DEFAULT_MESSAGE (custom)"))

        verify(emailSender).withSubject("$DEFAULT_SUBJECT (custom)")
        verify(emailSender).withMessage("$DEFAULT_MESSAGE (custom)")
    }

    @Test
    fun sendSimpleEmailFallsBackToTheDefaultsOnlyWhenNothingWasInformed() {
        service.sendSimpleEmail(request("ada@erudio.test", null, null))

        verify(emailSender).withSubject(DEFAULT_SUBJECT)
        verify(emailSender).withMessage(DEFAULT_MESSAGE)
        verify(emailSender).send(emailConfig)
    }

    @Test
    fun sendSimpleEmailTreatsBlankValuesAsMissing() {
        service.sendSimpleEmail(request("ada@erudio.test", "   ", ""))

        verify(emailSender).withSubject(DEFAULT_SUBJECT)
        verify(emailSender).withMessage(DEFAULT_MESSAGE)
    }

    @Test
    fun sendSimpleEmailDefaultsEachFieldIndependently() {
        service.sendSimpleEmail(request("ada@erudio.test", "Only the subject", null))
        verify(emailSender).withSubject("Only the subject")
        verify(emailSender).withMessage(DEFAULT_MESSAGE)

        reset(emailSender)

        service.sendSimpleEmail(request("ada@erudio.test", null, "Only the body"))
        verify(emailSender).withSubject(DEFAULT_SUBJECT)
        verify(emailSender).withMessage("Only the body")
    }

    @Test
    fun sendEmailWithAttachmentUsesTheValuesOfTheRequestAndAttachesTheFile() {
        val attachedPath = AtomicReference<Path>()
        val attachedContent = AtomicReference<String>()
        val attachedName = AtomicReference<String>()
        whenever(emailSender.attach(any(), any())).thenAnswer { invocation ->
            val path = Path.of(invocation.getArgument<String>(0))
            attachedPath.set(path)
            attachedName.set(invocation.getArgument(1))
            attachedContent.set(Files.readString(path))
            emailSender
        }

        service.setEmailWithAttachment(
            "{\"to\":\"ada@erudio.test\",\"subject\":\"Report\",\"body\":\"See the file\"}",
            attachment("report.txt", "the report")
        )

        verify(emailSender).to("ada@erudio.test")
        verify(emailSender).withSubject("Report")
        verify(emailSender).withMessage("See the file")
        verify(emailSender).send(emailConfig)

        assertEquals("the report", attachedContent.get(), "the file must exist with its content while the e-mail is sent")
        assertEquals("report.txt", attachedName.get(), "the recipient must see the name of the uploaded file, not the temporary one")
        assertNotEquals("report.txt", attachedPath.get().fileName.toString())
        assertFalse(Files.exists(attachedPath.get()), "the temporary copy must be deleted afterwards")
    }

    @Test
    fun sendEmailWithAttachmentFallsBackToTheDefaultsWhenTheRequestHasNoSubjectOrBody() {
        service.setEmailWithAttachment("{\"to\":\"ada@erudio.test\"}", attachment("a.txt", "x"))

        verify(emailSender).withSubject(DEFAULT_SUBJECT)
        verify(emailSender).withMessage(DEFAULT_MESSAGE)
        verify(emailSender).send(emailConfig)
    }

    @Test
    fun sendEmailWithAttachmentRejectsAnInvalidJson() {
        val exception = assertThrows(RuntimeException::class.java) {
            service.setEmailWithAttachment("{not json", attachment("a.txt", "x"))
        }

        assertEquals("Error parsing email request JSON!", exception.message)
        verifyNoInteractions(emailSender)
    }

    @Test
    fun sendEmailWithAttachmentRejectsUnknownFieldsInTheJson() {
        val exception = assertThrows(RuntimeException::class.java) {
            service.setEmailWithAttachment("{\"to\":\"ada@erudio.test\",\"cc\":\"bob@erudio.test\"}", attachment("a.txt", "x"))
        }

        assertEquals("Error parsing email request JSON!", exception.message)
        verifyNoInteractions(emailSender)
    }

    @Test
    fun sendEmailWithAttachmentReportsAFailureReadingTheAttachment() {
        val broken = mock<MultipartFile>()
        whenever(broken.originalFilename).thenReturn("broken.txt")
        doThrow(IOException("disk full")).whenever(broken).transferTo(any<File>())

        val exception = assertThrows(RuntimeException::class.java) {
            service.setEmailWithAttachment("{\"to\":\"ada@erudio.test\"}", broken)
        }

        assertEquals("Error processing the attachment!", exception.message)
        assertInstanceOf(IOException::class.java, exception.cause)
        verify(emailSender, never()).send(any())
    }

    @Test
    fun sendEmailWithAttachmentDeletesTheTemporaryFileEvenWhenTheSendFails() {
        val attachedPath = AtomicReference<Path>()
        whenever(emailSender.attach(any(), any())).thenAnswer { invocation ->
            attachedPath.set(Path.of(invocation.getArgument<String>(0)))
            emailSender
        }
        doThrow(IllegalStateException("smtp down")).whenever(emailSender).send(any())

        val exception = assertThrows(IllegalStateException::class.java) {
            service.setEmailWithAttachment("{\"to\":\"ada@erudio.test\"}", attachment("a.txt", "x"))
        }

        assertEquals("smtp down", exception.message)
        assertNotNull(attachedPath.get())
        assertFalse(Files.exists(attachedPath.get()))
    }
}
