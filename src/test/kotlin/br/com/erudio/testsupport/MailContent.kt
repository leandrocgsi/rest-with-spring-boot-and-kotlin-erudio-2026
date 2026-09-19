package br.com.erudio.testsupport

import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.MimeMessage

class MailContent private constructor() {

    private val htmlBodies: MutableList<String> = ArrayList()
    private val attachments: MutableMap<String, ByteArray> = LinkedHashMap()

    private fun collect(part: Part) {
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as Multipart
            for (i in 0 until multipart.count) {
                collect(multipart.getBodyPart(i))
            }
        } else if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) {
            part.inputStream.use { content ->
                attachments[part.fileName] = content.readAllBytes()
            }
        } else if (part.isMimeType("text/html")) {
            htmlBodies.add(part.content as String)
        }
    }

    fun html(): String = htmlBodies.joinToString("\n")

    fun attachments(): Map<String, ByteArray> = attachments

    companion object {

        fun of(message: MimeMessage): MailContent {
            val content = MailContent()
            content.collect(message)
            return content
        }
    }
}
