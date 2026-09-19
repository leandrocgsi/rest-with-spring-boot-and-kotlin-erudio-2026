package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.XmlRootElement
import java.util.*

@XmlRootElement
data class BookDTO(
    var id: Long? = null,
    var author: String? = null,
    var launchDate: Date? = null,
    var price: Double? = null,
    var title: String? = null
)
