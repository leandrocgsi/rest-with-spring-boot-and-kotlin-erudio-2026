package br.com.erudio.integrationtests.dto.wrappers.json

import br.com.erudio.integrationtests.dto.BookDTO
import com.fasterxml.jackson.annotation.JsonProperty

class BookEmbeddedDTO {

    @JsonProperty("books")
    var books: List<BookDTO>? = null
}
