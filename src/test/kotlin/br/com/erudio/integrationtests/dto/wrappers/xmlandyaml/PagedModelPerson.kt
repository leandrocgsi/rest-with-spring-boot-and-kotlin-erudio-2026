package br.com.erudio.integrationtests.dto.wrappers.xmlandyaml

import br.com.erudio.integrationtests.dto.PersonDTO
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
class PagedModelPerson {

    @XmlElement(name = "content")
    var content: List<PersonDTO>? = null
}
