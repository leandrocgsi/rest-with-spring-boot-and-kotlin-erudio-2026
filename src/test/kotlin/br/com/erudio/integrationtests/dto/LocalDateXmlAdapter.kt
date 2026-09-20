package br.com.erudio.integrationtests.dto

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import java.time.LocalDate

class LocalDateXmlAdapter : XmlAdapter<String?, LocalDate?>() {

    override fun unmarshal(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    override fun marshal(value: LocalDate?): String? = value?.toString()
}
