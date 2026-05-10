package swm.calender.core.api.controller.v1.match.service

import jakarta.validation.Validator
import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.match.request.CampaignCreateRequest
import swm.calender.core.api.controller.v1.match.request.CampaignStatusChangeRequest
import swm.calender.core.api.controller.v1.match.request.CandidateIdeaCreateRequest
import swm.calender.core.api.controller.v1.match.request.MatchRequestCreateRequest
import swm.calender.core.api.controller.v1.match.request.MatchRequestStatusChangeRequest
import swm.calender.core.api.controller.v1.match.request.ServiceProfileCreateRequest
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchSort

@Component
class MatchRequestValidator(
    private val validator: Validator,
) {
    fun validateServiceProfileCreate(request: ServiceProfileCreateRequest) {
        validateBean(request)
        if (request.name.isNullOrBlank()) throw MatchApiException.badRequest("name must not be blank.")
        if (request.summary.isNullOrBlank()) throw MatchApiException.badRequest("summary must not be blank.")
        if (request.description.isNullOrBlank()) throw MatchApiException.badRequest("description must not be blank.")
        request.category ?: throw MatchApiException.badRequest("category is required.")
        validatePlatforms(request.platforms)
    }

    fun validateCampaignCreate(request: CampaignCreateRequest) {
        validateBean(request)
        if (request.title.isNullOrBlank()) throw MatchApiException.badRequest("title must not be blank.")
        if (request.description.isNullOrBlank()) throw MatchApiException.badRequest("description must not be blank.")
        if ((request.targetTeamCount ?: 0) <= 0) {
            throw MatchApiException.badRequest("targetTeamCount must be positive.")
        }
        request.deadline ?: throw MatchApiException.badRequest("deadline is required.")
        request.reciprocalAvailable ?: throw MatchApiException.badRequest("reciprocalAvailable is required.")
    }

    fun validateCampaignStatusChange(request: CampaignStatusChangeRequest) {
        validateBean(request)
        request.status ?: throw MatchApiException.badRequest("status is required.")
    }

    fun validateCandidateIdeaCreate(request: CandidateIdeaCreateRequest) {
        validateBean(request)
        if (request.title.isNullOrBlank()) throw MatchApiException.badRequest("title must not be blank.")
        if (request.summary.isNullOrBlank()) throw MatchApiException.badRequest("summary must not be blank.")
        if (request.problem.isNullOrBlank()) throw MatchApiException.badRequest("problem must not be blank.")
        if (request.targetUsers.isNullOrBlank()) throw MatchApiException.badRequest("targetUsers must not be blank.")
        if (request.solution.isNullOrBlank()) throw MatchApiException.badRequest("solution must not be blank.")
        request.category ?: throw MatchApiException.badRequest("category is required.")
        validatePlatforms(request.platforms)
    }

    fun validateMatchRequestCreate(request: MatchRequestCreateRequest) {
        validateBean(request)
        request.type ?: throw MatchApiException.badRequest("type is required.")
    }

    fun validateMatchRequestStatusChange(request: MatchRequestStatusChangeRequest) {
        validateBean(request)
        request.status ?: throw MatchApiException.badRequest("status is required.")
    }

    fun parseCategory(value: String?): CampaignCategory? {
        return parseEnum<CampaignCategory>(value, "category")
    }

    fun parsePlatform(value: String?): Platform? {
        return parseEnum<Platform>(value, "platform")
    }

    fun parseReciprocalAvailable(value: String?): Boolean? {
        if (value == null) {
            return null
        }

        return when (value.lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw MatchApiException.badRequest("reciprocalAvailable must be true or false.")
        }
    }

    fun parseSort(value: String?): CampaignSearchSort {
        if (value.isNullOrBlank()) {
            return CampaignSearchSort.LATEST
        }

        return when (value.lowercase()) {
            "latest" -> CampaignSearchSort.LATEST
            "deadline" -> CampaignSearchSort.DEADLINE
            else -> throw MatchApiException.badRequest("sort must be latest or deadline.")
        }
    }

    fun parseId(
        value: Long,
        fieldName: String,
    ): Long {
        if (value <= 0) {
            throw MatchApiException.badRequest("$fieldName must be positive.")
        }

        return value
    }

    private fun validatePlatforms(platforms: List<Platform>?) {
        if (platforms.isNullOrEmpty()) {
            throw MatchApiException.badRequest("platforms must not be empty.")
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(
        value: String?,
        fieldName: String,
    ): T? {
        if (value.isNullOrBlank()) {
            return null
        }

        return enumValues<T>().firstOrNull { it.name == value.uppercase() }
            ?: throw MatchApiException.badRequest("$fieldName has an unsupported value.")
    }

    private fun validateBean(request: Any) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw MatchApiException.badRequest(violations.first().message)
        }
    }
}
