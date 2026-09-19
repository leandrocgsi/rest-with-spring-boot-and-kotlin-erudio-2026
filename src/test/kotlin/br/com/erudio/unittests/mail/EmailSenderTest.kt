package br.com.erudio.unittests.mail

import br.com.erudio.config.EmailConfig
import br.com.erudio.mail.EmailSender
import br.com.erudio.testsupport.MailContent
import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmailSenderTest {

    companion object {
        private const val SENDER = "sender@erudio.test"
    }

    @Mock
    private lateinit var mailSender: JavaMailSender

    private lateinit var config: EmailConfig
    private lateinit var sender: EmailSender

    @BeforeEach
    fun setUp() {
        config = EmailConfig()
        config.username = SENDER
        sender = EmailSender(mailSender)

        val session = Session.getInstance(Properties())
        lenient().`when`(mailSender.createMimeMessage()).thenAnswer { MimeMessage(session) }
    }

    private fun sentMessage(): MimeMessage {
        val captor = argumentCaptor<MimeMessage>()
        verify(mailSender).send(captor.capture())
        val message = captor.firstValue
        message.saveChanges()
        return message
    }

    private fun addressesOf(addresses: Array<Address>): List<String> {
        return addresses.map { it.toString() }
    }

    @Test
    fun sendsAnHtmlMessageFromTheConfiguredAccount() {
        sender.to("ada@erudio.test").withSubject("Welcome").withMessage("<h1>Hello Ada</h1>").send(config)

        val message = sentMessage()
        assertEquals("Welcome", message.subject)
        assertEquals(listOf(SENDER), addressesOf(message.from))
        assertEquals(listOf("ada@erudio.test"), addressesOf(message.getRecipients(Message.RecipientType.TO)))
        assertEquals("<h1>Hello Ada</h1>", MailContent.of(message).html())
    }

    @Test
    fun sendsToSeveralRecipientsSeparatedBySemicolonIgnoringSpaces() {
        sender.to("ada@erudio.test; bob@erudio.test ;carol@erudio.test")
            .withSubject("Team")
            .withMessage("Hi all")
            .send(config)

        assertEquals(
            listOf("ada@erudio.test", "bob@erudio.test", "carol@erudio.test"),
            addressesOf(sentMessage().getRecipients(Message.RecipientType.TO))
        )
    }

    @Test
    fun attachesTheGivenFile(@TempDir tempDir: Path) {
        val file = Files.writeString(tempDir.resolve("report.csv"), "id,name\n1,Ada\n", Charsets.UTF_8)

        sender.to("ada@erudio.test").withSubject("Report").withMessage("See attachment")
            .attach(file.toString())
            .send(config)

        val content = MailContent.of(sentMessage())
        assertEquals("See attachment", content.html())
        assertEquals(1, content.attachments().size)
        assertEquals("id,name\n1,Ada\n", String(content.attachments()["report.csv"]!!, Charsets.UTF_8))
    }

    @Test
    fun theRecipientSeesTheGivenAttachmentNameInsteadOfTheNameOfTheFile(@TempDir tempDir: Path) {
        val temporary = Files.writeString(tempDir.resolve("attachment8291746352report.csv"), "id\n1\n", Charsets.UTF_8)

        sender.to("ada@erudio.test").withSubject("Report").withMessage("See attachment")
            .attach(temporary.toString(), "Monthly report.csv")
            .send(config)

        val content = MailContent.of(sentMessage())
        assertEquals(listOf("Monthly report.csv"), content.attachments().keys.toList())
        assertEquals("id\n1\n", String(content.attachments()["Monthly report.csv"]!!, Charsets.UTF_8))
    }

    @Test
    fun aBlankAttachmentNameFallsBackToTheNameOfTheFile(@TempDir tempDir: Path) {
        val file = Files.writeString(tempDir.resolve("data.txt"), "x", Charsets.UTF_8)

        sender.to("ada@erudio.test").withSubject("Data").withMessage("See attachment")
            .attach(file.toString(), "  ")
            .send(config)

        assertEquals(listOf("data.txt"), MailContent.of(sentMessage()).attachments().keys.toList())
    }

    @Test
    fun sendsNoAttachmentWhenNoneWasGiven() {
        sender.to("ada@erudio.test").withSubject("Plain").withMessage("No files").send(config)

        assertTrue(MailContent.of(sentMessage()).attachments().isEmpty())
    }

    @Test
    fun keepsTheAccentsOfTheSubjectAndTheBody() {
        sender.to("ada@erudio.test").withSubject("Formação Spring Boot").withMessage("<p>Olá, João!</p>").send(config)

        val message = sentMessage()
        assertEquals("Formação Spring Boot", message.subject)
        assertEquals("<p>Olá, João!</p>", MailContent.of(message).html())
    }

    @Test
    fun canBeReusedForAnotherMessageAfterASend() {
        sender.to("ada@erudio.test").withSubject("First").withMessage("1").send(config)
        sender.to("bob@erudio.test").withSubject("Second").withMessage("2").send(config)

        val captor = argumentCaptor<MimeMessage>()
        verify(mailSender, times(2)).send(captor.capture())
        assertEquals("First", captor.allValues[0].subject)
        assertEquals("Second", captor.allValues[1].subject)
    }

    @Test
    fun rejectsAnInvalidRecipientAddress() {
        val exception = assertThrows(RuntimeException::class.java) { sender.to("not-an-address@@") }

        assertInstanceOf(AddressException::class.java, exception.cause)
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun propagatesAFailureOfTheMailServer() {
        doThrow(MailSendException("connection refused")).whenever(mailSender).send(any<MimeMessage>())

        val exception = assertThrows(MailSendException::class.java) {
            sender.to("ada@erudio.test").withSubject("Down").withMessage("x").send(config)
        }

        assertEquals("connection refused", exception.message)
    }
}
