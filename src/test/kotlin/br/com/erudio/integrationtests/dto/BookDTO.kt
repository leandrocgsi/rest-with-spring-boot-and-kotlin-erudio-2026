package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.time.LocalDate

@XmlRootElement
data class BookDTO(
    var id: Long? = null,
    var author: String? = null,
    @get:XmlJavaTypeAdapter(LocalDateXmlAdapter::class)
    var launchDate: LocalDate? = null,
    var price: Double? = null,
    var title: String? = null
)
