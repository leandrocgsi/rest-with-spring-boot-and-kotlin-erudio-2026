package br.com.erudio.controllers

import br.com.erudio.controllers.docs.FileControllerDocs
import br.com.erudio.data.dto.UploadFileResponseDTO
import br.com.erudio.services.FileStorageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.logging.Logger

@RestController
@RequestMapping("/api/file/v1")
class FileController : FileControllerDocs {

    private val logger = Logger.getLogger(FileController::class.java.name)

    @Autowired
    private lateinit var service: FileStorageService

    @PostMapping("/uploadFile")
    override fun uploadFile(@RequestParam("file") file: MultipartFile): UploadFileResponseDTO {
        val fileName = service.storeFile(file)

        val fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/file/v1/downloadFile/")
            .path(fileName)
            .toUriString()

        return UploadFileResponseDTO(fileName, fileDownloadUri, file.contentType, file.size)
    }

    @PostMapping("/uploadMultipleFiles")
    override fun uploadMultipleFiles(@RequestParam("files") files: Array<MultipartFile>): List<UploadFileResponseDTO> {
        return files.map { file -> uploadFile(file) }
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    override fun downloadFile(@PathVariable fileName: String, request: HttpServletRequest): ResponseEntity<Resource> {
        val resource = service.loadFileAsResource(fileName)
        var contentType: String? = null
        try {
            contentType = request.servletContext.getMimeType(resource.file.absolutePath)
        } catch (e: Exception) {
            logger.severe("Could not determine file type!")
        }

        if (contentType == null) {
            contentType = "application/octet-stream"
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.filename + "\""
            )
            .body(resource)
    }
}
