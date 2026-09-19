package br.com.erudio.controllers

import br.com.erudio.controllers.docs.EmailControllerDocs
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.services.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/email/v1")
class EmailController : EmailControllerDocs {

    @Autowired
    private lateinit var service: EmailService

    @PostMapping
    override fun sendEmail(@RequestBody emailRequest: EmailRequestDTO): ResponseEntity<String> {
        service.sendSimpleEmail(emailRequest)
        return ResponseEntity("e-Mail sent with success!", HttpStatus.OK)
    }

    @PostMapping(value = ["/withAttachment"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun sendEmailWithAttachment(
        @RequestParam("emailRequest") emailRequest: String,
        @RequestParam("attachment") attachment: MultipartFile
    ): ResponseEntity<String> {
        service.setEmailWithAttachment(emailRequest, attachment)
        return ResponseEntity("e-Mail with attachment sent successfully!", HttpStatus.OK)
    }
}
