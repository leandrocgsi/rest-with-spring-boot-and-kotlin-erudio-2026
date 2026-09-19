package br.com.erudio.integrationtests.dto.wrappers.xmlandyaml

import br.com.erudio.integrationtests.dto.BookDTO
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
class PagedModelBook {

    @XmlElement(name = "content")
    var content: List<BookDTO>? = null
}
