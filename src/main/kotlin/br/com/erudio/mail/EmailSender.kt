package br.com.erudio.mail

import br.com.erudio.config.EmailConfig
import jakarta.mail.MessagingException
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.io.File
import java.util.logging.Logger

@Component
class EmailSender(private val mailSender: JavaMailSender) {

    private val logger = Logger.getLogger(EmailSender::class.java.name)

    private var to: String? = null
    private var subject: String? = null
    private var body: String? = null
    private var recipients: List<InternetAddress> = ArrayList()
    private var attachment: File? = null
    private var attachmentName: String? = null

    fun to(to: String): EmailSender {
        this.to = to
        this.recipients = getRecipients(to)
        return this
    }

    fun withSubject(subject: String?): EmailSender {
        this.subject = subject
        return this
    }

    fun withMessage(body: String?): EmailSender {
        this.body = body
        return this
    }

    fun attach(fileDir: String): EmailSender {
        return attach(fileDir, null)
    }

    fun attach(fileDir: String, attachmentName: String?): EmailSender {
        val file = File(fileDir)
        this.attachment = file
        this.attachmentName = if (StringUtils.hasText(attachmentName)) attachmentName else file.name
        return this
    }

    fun send(config: EmailConfig) {
        val message = mailSender.createMimeMessage()
        try {
            val helper = MimeMessageHelper(message, true)
            helper.setFrom(config.username!!)
            helper.setTo(recipients.toTypedArray())
            helper.setSubject(subject!!)
            helper.setText(body!!, true)
            attachment?.let { helper.addAttachment(attachmentName!!, it) }
            mailSender.send(message)
            logger.info("Email sent to $to with the subject '$subject'")
            reset()
        } catch (e: MessagingException) {
            throw RuntimeException("Error sending the email", e)
        }
    }

    private fun reset() {
        this.to = null
        this.subject = null
        this.body = null
        this.recipients = ArrayList()
        this.attachment = null
        this.attachmentName = null
    }

    private fun getRecipients(to: String): List<InternetAddress> {
        return to.replace("\\s".toRegex(), "")
            .split(";")
            .filter { it.isNotEmpty() }
            .map {
                try {
                    InternetAddress(it)
                } catch (e: AddressException) {
                    throw RuntimeException(e)
                }
            }
    }
}
