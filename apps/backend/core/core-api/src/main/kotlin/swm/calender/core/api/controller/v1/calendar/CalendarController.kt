package swm.calender.core.api.controller.v1.calendar

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.api.controller.v1.calendar.request.MentoringScheduleBulkPushRequest
import swm.calender.core.api.controller.v1.calendar.request.When2meetLinkRequest
import swm.calender.core.api.controller.v1.calendar.response.MentoringScheduleBulkPushResponse
import swm.calender.core.api.controller.v1.calendar.response.UnifiedAvailabilityResponse
import swm.calender.core.api.controller.v1.calendar.response.When2meetLinkResponse
import swm.calender.core.api.controller.v1.calendar.service.CalendarApiException
import swm.calender.core.api.controller.v1.calendar.service.CalendarApiFacade
import swm.calender.core.api.controller.v1.calendar.service.CalendarRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.error.ErrorType
import swm.calender.core.support.response.ApiResponse
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage

@RestController
class CalendarController(
    private val calendarApiFacade: CalendarApiFacade,
    private val calendarRequestValidator: CalendarRequestValidator,
) {
    @PostMapping("/api/v1/calendar/mentoring-schedules:bulk-push")
    fun bulkPushMentoringSchedules(
        authentication: Authentication?,
        @RequestBody request: MentoringScheduleBulkPushRequest,
    ): ResponseEntity<ApiResponse<MentoringScheduleBulkPushResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            calendarRequestValidator.validateBulkPush(request)
            MentoringScheduleBulkPushResponse.from(
                calendarApiFacade.bulkPushMentoringSchedules(user, request),
            )
        }
    }

    @PutMapping("/api/v1/calendar/when2meet-link")
    fun putWhen2meetLink(
        authentication: Authentication?,
        @RequestBody request: When2meetLinkRequest,
    ): ResponseEntity<ApiResponse<When2meetLinkResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            calendarRequestValidator.validateWhen2meetLink(request)
            When2meetLinkResponse.from(
                calendarApiFacade.registerWhen2meetLink(user, request),
            )
        }
    }

    @GetMapping("/api/v1/calendar/availability")
    fun getUnifiedAvailability(
        authentication: Authentication?,
        @RequestParam startsAt: String?,
        @RequestParam endsAt: String?,
    ): ResponseEntity<ApiResponse<UnifiedAvailabilityResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            val (parsedStartsAt, parsedEndsAt) = calendarRequestValidator.parseRange(startsAt, endsAt)
            UnifiedAvailabilityResponse.from(
                calendarApiFacade.getUnifiedAvailability(
                    user = user,
                    startsAt = parsedStartsAt,
                    endsAt = parsedEndsAt,
                ),
            )
        }
    }

    private fun <T> handle(action: () -> T): ResponseEntity<ApiResponse<T>> {
        return try {
            ResponseEntity.ok(ApiResponse.success(action()))
        } catch (e: CalendarApiException) {
            ResponseEntity
                .status(e.errorType.status)
                .body(ApiResponse.error(e.errorType, mapOf("reason" to e.message)))
        } catch (e: CalendarDomainException) {
            val errorType = e.errorMessage.toErrorType()
            ResponseEntity
                .status(errorType.status)
                .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
        } catch (e: TeamDomainException) {
            val errorType = e.errorMessage.toErrorType()
            ResponseEntity
                .status(errorType.status)
                .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
        }
    }

    private fun <T> unauthorized(): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorType.AUTHENTICATION_REQUIRED))
    }

    private fun CalendarErrorMessage.toErrorType(): ErrorType {
        return when (this) {
            CalendarErrorMessage.CALENDAR_SUB_SERVICE_DISABLED -> ErrorType.SUB_SERVICE_DISABLED
            CalendarErrorMessage.TEAM_CALENDAR_AUTH_REQUIRED -> ErrorType.INVALID_TEAM_STATE
            CalendarErrorMessage.TEAM_CALENDAR_NOT_FOUND -> ErrorType.RESOURCE_NOT_FOUND
            CalendarErrorMessage.TEAM_CALENDAR_ID_REQUIRED,
            CalendarErrorMessage.MENTORING_SCHEDULE_EXTERNAL_SOURCE_ID_REQUIRED,
            CalendarErrorMessage.MENTORING_SCHEDULE_TITLE_REQUIRED,
            CalendarErrorMessage.GOOGLE_EVENT_ID_REQUIRED,
            CalendarErrorMessage.WHEN2MEET_URL_REQUIRED,
            CalendarErrorMessage.WHEN2MEET_URL_INVALID,
            CalendarErrorMessage.WHEN2MEET_FAILURE_REASON_REQUIRED,
            CalendarErrorMessage.AVAILABILITY_COUNT_NEGATIVE,
            -> ErrorType.VALIDATION_ERROR
        }
    }

    private fun TeamErrorMessage.toErrorType(): ErrorType {
        return when (this) {
            TeamErrorMessage.TEAM_OWNER_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_REQUIRED,
            -> ErrorType.FORBIDDEN

            TeamErrorMessage.TEAM_NOT_FOUND,
            TeamErrorMessage.INVALID_INVITE_CODE,
            TeamErrorMessage.TEAM_MEMBER_NOT_FOUND,
            -> ErrorType.RESOURCE_NOT_FOUND

            TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS,
            TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER,
            -> ErrorType.DUPLICATE_RESOURCE

            TeamErrorMessage.TEAM_NAME_REQUIRED,
            TeamErrorMessage.INVITE_CODE_REQUIRED,
            TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_NAME_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_EMAIL_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_INACTIVE,
            TeamErrorMessage.TEAM_NOT_PERSISTED,
            -> ErrorType.VALIDATION_ERROR
        }
    }
}
