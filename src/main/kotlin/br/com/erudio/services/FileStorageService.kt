package br.com.erudio.services

import br.com.erudio.config.FileStorageConfig
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

@Service
class FileStorageService @Autowired constructor(fileStorageConfig: FileStorageConfig) {

    private val logger = Logger.getLogger(FileStorageService::class.java.name)

    private val fileStorageLocation: Path

    init {
        fileStorageLocation = Paths.get(fileStorageConfig.uploadDir).toAbsolutePath().normalize()
        try {
            logger.info("Creating Directories")
            Files.createDirectories(fileStorageLocation)
        } catch (e: Exception) {
            logger.severe("Could not create the directory where files will be stored!")
            throw FileStorageException("Could not create the directory where files will be stored!", e)
        }
    }

    fun storeFile(file: MultipartFile): String {

        val fileName = StringUtils.cleanPath(file.originalFilename!!)

        return try {
            if (fileName.contains("..")) {
                logger.severe("Sorry! Filename Contains a Invalid path Sequence $fileName")
                throw FileStorageException("Sorry! Filename Contains a Invalid path Sequence $fileName")
            }

            logger.info("Saving file in Disk")

            val targetLocation = fileStorageLocation.resolve(fileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            fileName
        } catch (e: Exception) {
            logger.severe("Could not store file $fileName. Please try Again!")
            throw FileStorageException("Could not store file $fileName. Please try Again!", e)
        }
    }

    fun loadFileAsResource(fileName: String): Resource {
        return try {
            val filePath = fileStorageLocation.resolve(fileName).normalize()
            val resource: Resource = UrlResource(filePath.toUri())
            if (resource.exists()) {
                resource
            } else {
                logger.severe("File not found $fileName")
                throw FileNotFoundException("File not found $fileName")
            }
        } catch (e: Exception) {
            logger.severe("File not found $fileName")
            throw FileNotFoundException("File not found $fileName", e)
        }
    }
}
