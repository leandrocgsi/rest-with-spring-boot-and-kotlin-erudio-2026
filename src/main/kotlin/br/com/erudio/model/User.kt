package br.com.erudio.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

@Entity
@Table(name = "users")
class User : UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "user_name", unique = true)
    var userName: String? = null

    @Column(name = "full_name")
    var fullName: String? = null

    @Column
    private var password: String? = null

    @Column(name = "account_non_expired")
    var accountNonExpired: Boolean? = null

    @Column(name = "account_non_locked")
    var accountNonLocked: Boolean? = null

    @Column(name = "credentials_non_expired")
    var credentialsNonExpired: Boolean? = null

    @Column
    var enabled: Boolean? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_permission",
        joinColumns = [JoinColumn(name = "id_user")],
        inverseJoinColumns = [JoinColumn(name = "id_permission")]
    )
    var permissions: List<Permission>? = null

    val roles: List<String?>
        get() = permissions!!.map { it.description }

    override fun getAuthorities(): Collection<GrantedAuthority> = permissions!!

    override fun getPassword(): String? = password

    fun setPassword(password: String?) {
        this.password = password
    }

    override fun getUsername(): String = userName!!

    override fun isAccountNonExpired(): Boolean = accountNonExpired!!

    override fun isAccountNonLocked(): Boolean = accountNonLocked!!

    override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired!!

    override fun isEnabled(): Boolean = enabled!!

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        other as User
        return id == other.id && userName == other.userName && fullName == other.fullName &&
                password == other.password && accountNonExpired == other.accountNonExpired &&
                accountNonLocked == other.accountNonLocked &&
                credentialsNonExpired == other.credentialsNonExpired &&
                enabled == other.enabled && permissions == other.permissions
    }

    override fun hashCode(): Int = Objects.hash(
        id, userName, fullName, password, accountNonExpired,
        accountNonLocked, credentialsNonExpired, enabled, permissions
    )
}
