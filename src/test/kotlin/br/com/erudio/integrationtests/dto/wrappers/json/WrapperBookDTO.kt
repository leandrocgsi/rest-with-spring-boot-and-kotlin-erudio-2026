package br.com.erudio.integrationtests.dto.wrappers.json

import com.fasterxml.jackson.annotation.JsonProperty

class WrapperBookDTO {

    @JsonProperty("_embedded")
    var embedded: BookEmbeddedDTO? = null
}
