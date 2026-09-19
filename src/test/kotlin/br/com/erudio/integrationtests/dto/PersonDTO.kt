package br.com.erudio.integrationtests.dto

import br.com.erudio.model.Book
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
data class PersonDTO(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var address: String? = null,
    var gender: String? = null,
    var enabled: Boolean? = null,
    var profileUrl: String? = null,
    var photoUrl: String? = null,
    var books: List<Book>? = null
)
