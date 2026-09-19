package br.com.erudio.integrationtests.dto.wrappers.json

import com.fasterxml.jackson.annotation.JsonProperty

class WrapperPersonDTO {

    @JsonProperty("_embedded")
    var embedded: PersonEmbeddedDTO? = null
}
