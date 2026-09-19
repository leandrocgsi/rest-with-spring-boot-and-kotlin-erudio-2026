package br.com.erudio.services

import br.com.erudio.config.EmailConfig
import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailSender
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.io.IOException

@Service
class EmailService {

    @Autowired
    private lateinit var emailSender: EmailSender

    @Autowired
    private lateinit var emailConfigs: EmailConfig

    @Autowired
    private lateinit var emailDefaults: EmailDefaultsConfig

    fun sendSimpleEmail(emailRequest: EmailRequestDTO) {
        emailSender
            .to(emailRequest.to!!)
            .withSubject(subjectOf(emailRequest))
            .withMessage(messageOf(emailRequest))
            .send(emailConfigs)
    }

    fun setEmailWithAttachment(emailRequestJson: String, attachment: MultipartFile) {
        var tempFile: File? = null
        try {
            val emailRequest = JsonMapper.builderWithJackson2Defaults().build()
                .readValue(emailRequestJson, EmailRequestDTO::class.java)
            tempFile = File.createTempFile("attachment", attachment.originalFilename)
            attachment.transferTo(tempFile)

            emailSender
                .to(emailRequest.to!!)
                .withSubject(subjectOf(emailRequest))
                .withMessage(messageOf(emailRequest))
                .attach(tempFile.absolutePath, attachment.originalFilename)
                .send(emailConfigs)

        } catch (e: JacksonException) {
            throw RuntimeException("Error parsing email request JSON!", e)
        } catch (e: IOException) {
            throw RuntimeException("Error processing the attachment!", e)
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete()
        }
    }

    private fun subjectOf(emailRequest: EmailRequestDTO): String? {
        return if (StringUtils.hasText(emailRequest.subject)) emailRequest.subject else emailDefaults.subject
    }

    private fun messageOf(emailRequest: EmailRequestDTO): String? {
        return if (StringUtils.hasText(emailRequest.body)) emailRequest.body else emailDefaults.message
    }
}
