package br.com.erudio.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "aws.s3")
class AwsS3Properties(
    var bucket: String = "",
    var region: String = "",
    var endpoint: String = "",
    var accessKey: String = "",
    var secretKey: String = ""
)
