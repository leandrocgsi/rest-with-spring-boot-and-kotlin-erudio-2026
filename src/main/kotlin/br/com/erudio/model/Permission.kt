package br.com.erudio.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority

@Entity
@Table(name = "permission")
data class Permission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column
    var description: String? = null
) : GrantedAuthority {

    override fun getAuthority(): String? = description
}
