package br.com.erudio.data.dto

import br.com.erudio.model.Book
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "people")
data class PersonDTO(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var address: String? = null,
    var gender: String? = null,
    var enabled: Boolean? = null,
    var profileUrl: String? = null,
    var photoUrl: String? = null,

    @field:JsonIgnore
    var books: List<Book>? = null
) : RepresentationModel<PersonDTO>() {

    @get:JsonIgnore
    val name: String
        get() = (firstName ?: "") + (if (lastName != null) " $lastName" else "")
}
