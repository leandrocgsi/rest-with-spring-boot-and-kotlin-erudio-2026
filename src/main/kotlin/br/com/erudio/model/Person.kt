package br.com.erudio.model

import jakarta.persistence.*

@Entity
@Table(name = "person")
data class Person(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "first_name", nullable = false, length = 80)
    var firstName: String? = null,

    @Column(name = "last_name", nullable = false, length = 80)
    var lastName: String? = null,

    @Column(nullable = false, length = 100)
    var address: String? = null,

    @Column(nullable = false, length = 6)
    var gender: String? = null,

    @Column(nullable = false)
    var enabled: Boolean? = null,

    @Column(name = "wikipedia_profile_url", length = 255)
    var profileUrl: String? = null,

    @Column(name = "photo_url", length = 255)
    var photoUrl: String? = null,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "person_books",
        joinColumns = [JoinColumn(name = "person_id")],
        inverseJoinColumns = [JoinColumn(name = "book_id")]
    )
    var books: List<Book>? = null
)
