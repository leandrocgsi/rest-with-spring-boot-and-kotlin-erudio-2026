package br.com.erudio.data.dto

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.time.LocalDate

@Relation(collectionRelation = "books")
data class BookDTO(
    var id: Long? = null,
    var author: String? = null,
    var launchDate: LocalDate? = null,
    var price: Double? = null,
    var title: String? = null
) : RepresentationModel<BookDTO>()
