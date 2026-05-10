package swm.calender.core.api.controller.v1.match

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.match.response.NotificationListResponse
import swm.calender.core.api.controller.v1.match.service.NotificationApiFacade
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class NotificationController(
    private val notificationApiFacade: NotificationApiFacade,
) {
    @GetMapping("/api/v1/notifications")
    fun listNotifications(
        authentication: Authentication?,
    ): ResponseEntity<ApiResponse<NotificationListResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            NotificationListResponse.from(notificationApiFacade.listNotifications(user))
        }
    }
}
