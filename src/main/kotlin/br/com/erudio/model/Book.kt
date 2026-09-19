package br.com.erudio.model

import jakarta.persistence.*
import java.util.*

@Suppress("DEPRECATION")
@Entity
@Table(name = "books")
data class Book(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 180)
    var author: String? = null,

    @Column(name = "launch_date", nullable = false)
    @Temporal(TemporalType.DATE)
    var launchDate: Date? = null,

    @Column(nullable = false)
    var price: Double? = null,

    @Column(nullable = false, length = 250)
    var title: String? = null
)
