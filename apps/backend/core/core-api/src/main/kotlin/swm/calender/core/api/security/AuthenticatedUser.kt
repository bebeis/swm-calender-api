package swm.calender.core.api.security

import org.springframework.security.core.Authentication
import swm.calender.core.common.id.UserId

data class AuthenticatedUser(
    val userId: UserId,
    val email: String,
    val name: String,
) {
    companion object {
        fun from(authentication: Authentication?): AuthenticatedUser? {
            if (authentication?.principal !is AuthenticatedUser) {
                return null
            }
            return authentication.principal as AuthenticatedUser
        }
    }
}
