package br.com.erudio.controllers.docs

import br.com.erudio.data.dto.UploadFileResponseDTO
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "File Endpoint")
interface FileControllerDocs {

    fun uploadFile(file: MultipartFile): UploadFileResponseDTO

    fun uploadMultipleFiles(files: Array<MultipartFile>): List<UploadFileResponseDTO>

    fun downloadFile(
        fileName: String,
        request: HttpServletRequest
    ): ResponseEntity<Resource>
}
