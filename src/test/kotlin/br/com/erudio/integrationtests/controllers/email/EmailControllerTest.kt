package br.com.erudio.integrationtests.controllers.email

import br.com.erudio.integrationtests.AuthenticatedIntegrationTest
import br.com.erudio.testsupport.MailContent
import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse
import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.internet.MimeMessage
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class EmailControllerTest : AuthenticatedIntegrationTest() {

    companion object {
        private const val BASE = "/api/email/v1"
        private const val ATTACHMENT_URL = "$BASE/withAttachment"
        private const val SENDER = "sender@erudio.test"

        private const val DEFAULT_SUBJECT = "Default Subject"
        private const val DEFAULT_MESSAGE = "Default Message"

        private fun sendSimple(request: Map<String, Any>) {
            given().spec(authenticated())
                .contentType("application/json")
                .body(request)
                .`when`()
                .post(BASE)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(equalTo("e-Mail sent with success!"))
        }

        private fun sendWithAttachment(requestJson: String, attachmentName: String, attachment: ByteArray): ValidatableResponse {
            return given().spec(authenticated())
                .multiPart("emailRequest", requestJson)
                .multiPart("attachment", attachmentName, attachment, "application/octet-stream")
                .`when`()
                .post(ATTACHMENT_URL)
                .then()
                .log().ifValidationFails()
        }

        private fun recipientsOf(message: MimeMessage): List<String> {
            return message.getRecipients(Message.RecipientType.TO).map(Address::toString)
        }
    }

    @BeforeEach
    fun emptyTheMailboxes() {
        greenMail().purgeEmailFromAllMailboxes()
    }

    private fun onlyMessage(): MimeMessage {
        assertTrue(greenMail().waitForIncomingEmail(5000, 1), "the e-mail never arrived at the SMTP server")
        val messages = greenMail().receivedMessages
        assertEquals(1, messages.size)
        return messages[0]
    }

    @Test
    fun theSubjectAndTheBodyOfTheRequestArriveAsTheyWereSent() {
        sendSimple(mapOf("to" to "ada@erudio.test", "subject" to "Welcome to the course", "body" to "<h1>Hello Ada</h1><p>See you in class.</p>"))

        val message = onlyMessage()
        assertEquals("Welcome to the course", message.subject)
        assertEquals(SENDER, message.from[0].toString())
        assertEquals(listOf("ada@erudio.test"), recipientsOf(message))
        assertEquals("<h1>Hello Ada</h1><p>See you in class.</p>", MailContent.of(message).html())
        assertTrue(MailContent.of(message).attachments().isEmpty())
    }

    @Test
    fun accentsSurviveTheTripThroughTheMailServer() {
        sendSimple(mapOf("to" to "joao@erudio.test", "subject" to "Formação Spring Boot com Kotlin", "body" to "<p>Olá, João! Até a próxima aula.</p>"))

        val message = onlyMessage()
        assertEquals("Formação Spring Boot com Kotlin", message.subject)
        assertEquals("<p>Olá, João! Até a próxima aula.</p>", MailContent.of(message).html())
    }

    @Test
    fun severalRecipientsSeparatedBySemicolonAllReceiveTheMessage() {
        sendSimple(mapOf("to" to "ada@erudio.test; bob@erudio.test ;carol@erudio.test", "subject" to "Team", "body" to "Hi all"))

        assertTrue(greenMail().waitForIncomingEmail(5000, 3))
        val messages = greenMail().receivedMessages
        assertEquals(3, messages.size)
        val everyone = listOf("ada@erudio.test", "bob@erudio.test", "carol@erudio.test")
        for (message in messages) {
            assertEquals(everyone, recipientsOf(message), "each copy names all the recipients")
            assertEquals("Team", message.subject)
        }
    }

    @Test
    fun theDefaultsAreUsedOnlyForTheFieldsTheRequestLeavesOut() {
        sendSimple(mapOf("to" to "ada@erudio.test"))

        val message = onlyMessage()
        assertEquals(DEFAULT_SUBJECT, message.subject)
        assertEquals(DEFAULT_MESSAGE, MailContent.of(message).html())
    }

    @Test
    fun onlyTheSubjectInformedKeepsTheSubjectAndDefaultsTheMessage() {
        sendSimple(mapOf("to" to "ada@erudio.test", "subject" to "My own subject"))

        val message = onlyMessage()
        assertEquals("My own subject", message.subject)
        assertEquals(DEFAULT_MESSAGE, MailContent.of(message).html())
    }

    @Test
    fun onlyTheBodyInformedKeepsTheBodyAndDefaultsTheSubject() {
        sendSimple(mapOf("to" to "ada@erudio.test", "body" to "My own body"))

        val message = onlyMessage()
        assertEquals(DEFAULT_SUBJECT, message.subject)
        assertEquals("My own body", MailContent.of(message).html())
    }

    @Test
    fun blankValuesCountAsNotInformed() {
        sendSimple(mapOf("to" to "ada@erudio.test", "subject" to "   ", "body" to ""))

        val message = onlyMessage()
        assertEquals(DEFAULT_SUBJECT, message.subject)
        assertEquals(DEFAULT_MESSAGE, MailContent.of(message).html())
    }

    @Test
    fun anInvalidRecipientFailsAndNothingIsDelivered() {
        given().spec(authenticated())
            .contentType("application/json")
            .body(mapOf("to" to "not-an-address@@", "subject" to "x", "body" to "y"))
            .`when`()
            .post(BASE)
            .then()
            .statusCode(500)

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun aMalformedJsonBodyIsABadRequest() {
        given().spec(authenticated())
            .contentType("application/json")
            .body("{not json")
            .`when`()
            .post(BASE)
            .then()
            .statusCode(400)

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun theAttachmentArrivesWithTheSubjectAndBodyOfTheRequest() {
        val report = "id,name\n1,Ada\n2,Alan\n".toByteArray(Charsets.UTF_8)

        sendWithAttachment(
            "{\"to\":\"ada@erudio.test\",\"subject\":\"Monthly report\",\"body\":\"<p>The report is attached.</p>\"}",
            "report.csv", report
        )
            .statusCode(200)
            .body(equalTo("e-Mail with attachment sent successfully!"))

        val message = onlyMessage()
        assertEquals("Monthly report", message.subject)
        assertEquals(listOf("ada@erudio.test"), recipientsOf(message))
        val content = MailContent.of(message)
        assertEquals("<p>The report is attached.</p>", content.html())
        assertEquals(listOf("report.csv"), content.attachments().keys.toList())
        assertEquals(
            String(report, Charsets.UTF_8),
            String(content.attachments()["report.csv"]!!, Charsets.UTF_8).replace("\r\n", "\n")
        )
    }

    @Test
    fun aBinaryAttachmentSurvivesByteForByte() {
        val binary = ByteArray(300 * 1024)
        Random(7).nextBytes(binary)

        sendWithAttachment("{\"to\":\"ada@erudio.test\",\"subject\":\"Binary\",\"body\":\"see file\"}", "data.bin", binary)
            .statusCode(200)

        val content = MailContent.of(onlyMessage())
        assertArrayEquals(binary, content.attachments()["data.bin"])
    }

    @Test
    fun theAttachmentEmailFallsBackToTheDefaultsWhenTheRequestHasNoSubjectOrBody() {
        sendWithAttachment("{\"to\":\"ada@erudio.test\"}", "notes.txt", "notes".toByteArray(Charsets.UTF_8))
            .statusCode(200)

        val message = onlyMessage()
        assertEquals(DEFAULT_SUBJECT, message.subject)
        val content = MailContent.of(message)
        assertEquals(DEFAULT_MESSAGE, content.html())
        assertArrayEquals("notes".toByteArray(Charsets.UTF_8), content.attachments()["notes.txt"])
    }

    @Test
    fun theAttachmentEmailKeepsAnySubjectTheCallerSets() {
        sendWithAttachment("{\"to\":\"ada@erudio.test\",\"subject\":\"Only my subject\"}", "notes.txt", "n".toByteArray(Charsets.UTF_8))
            .statusCode(200)

        val message = onlyMessage()
        assertEquals("Only my subject", message.subject)
        assertEquals(DEFAULT_MESSAGE, MailContent.of(message).html())
    }

    @Test
    fun anInvalidRequestJsonIsRejectedAndNothingIsDelivered() {
        sendWithAttachment("{not json", "notes.txt", "n".toByteArray(Charsets.UTF_8))
            .statusCode(500)
            .body("message", equalTo("Error parsing email request JSON!"))

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun unknownFieldsInTheRequestJsonAreRejected() {
        sendWithAttachment("{\"to\":\"ada@erudio.test\",\"cc\":\"bob@erudio.test\"}", "notes.txt", "n".toByteArray(Charsets.UTF_8))
            .statusCode(500)
            .body("message", equalTo("Error parsing email request JSON!"))

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun anInvalidRecipientIsRejectedAndNothingIsDelivered() {
        sendWithAttachment("{\"to\":\"not-an-address@@\",\"subject\":\"x\"}", "notes.txt", "n".toByteArray(Charsets.UTF_8))
            .statusCode(500)

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun theAttachmentPartIsRequired() {
        given().spec(authenticated())
            .multiPart("emailRequest", "{\"to\":\"ada@erudio.test\"}")
            .`when`()
            .post(ATTACHMENT_URL)
            .then()
            .statusCode(400)

        assertEquals(0, greenMail().receivedMessages.size)
    }

    @Test
    fun theRequestPartIsRequired() {
        given().spec(authenticated())
            .multiPart("attachment", "notes.txt", "n".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post(ATTACHMENT_URL)
            .then()
            .statusCode(400)
    }

    @Test
    fun theAttachmentEndpointOnlyAcceptsMultipart() {
        given().spec(authenticated())
            .contentType("application/json")
            .body("{\"to\":\"ada@erudio.test\"}")
            .`when`()
            .post(ATTACHMENT_URL)
            .then()
            .statusCode(415)
    }

    @Test
    fun bothEndpointsRequireAuthentication() {
        given().spec(anonymous())
            .contentType("application/json")
            .body(mapOf("to" to "ada@erudio.test"))
            .`when`()
            .post(BASE)
            .then()
            .statusCode(403)

        given().spec(anonymous())
            .multiPart("emailRequest", "{\"to\":\"ada@erudio.test\"}")
            .multiPart("attachment", "notes.txt", "n".toByteArray(Charsets.UTF_8), "text/plain")
            .`when`()
            .post(ATTACHMENT_URL)
            .then()
            .statusCode(403)

        assertEquals(0, greenMail().receivedMessages.size)
    }
}
