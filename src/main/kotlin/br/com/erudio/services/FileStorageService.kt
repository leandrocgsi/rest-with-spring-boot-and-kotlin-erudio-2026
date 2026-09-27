package br.com.erudio.services

import br.com.erudio.config.AwsS3Properties
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.util.logging.Logger

@Service
class FileStorageService @Autowired constructor(
    private val s3Client: S3Client,
    properties: AwsS3Properties
) {

    private val logger = Logger.getLogger(FileStorageService::class.java.name)

    private val bucket: String = properties.bucket

    init {
        createBucketIfMissing()
    }

    private fun createBucketIfMissing() {
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
            } catch (e: NoSuchBucketException) {
                logger.info("Creating S3 bucket $bucket")
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
            }
        } catch (e: Exception) {
            logger.severe("Could not verify or create the S3 bucket where files will be stored!")
            throw FileStorageException("Could not verify or create the S3 bucket where files will be stored!", e)
        }
    }

    fun storeFile(file: MultipartFile): String {

        val fileName = StringUtils.cleanPath(file.originalFilename!!)

        return try {
            if (fileName.contains("..")) {
                logger.severe("Sorry! Filename Contains a Invalid path Sequence $fileName")
                throw FileStorageException("Sorry! Filename Contains a Invalid path Sequence $fileName")
            }

            logger.info("Saving file in S3")

            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.contentType)
                .build()

            s3Client.putObject(request, RequestBody.fromBytes(file.bytes))
            fileName
        } catch (e: Exception) {
            logger.severe("Could not store file $fileName. Please try Again!")
            throw FileStorageException("Could not store file $fileName. Please try Again!", e)
        }
    }

    fun loadFileAsResource(fileName: String): Resource {
        return try {
            val content = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(fileName).build()
            ).asByteArray()

            object : ByteArrayResource(content) {
                override fun getFilename(): String = fileName
                override fun getFile(): File = File(fileName)
                override fun exists(): Boolean = true
            }
        } catch (e: Exception) {
            logger.severe("File not found $fileName")
            throw FileNotFoundException("File not found $fileName", e)
        }
    }
}
