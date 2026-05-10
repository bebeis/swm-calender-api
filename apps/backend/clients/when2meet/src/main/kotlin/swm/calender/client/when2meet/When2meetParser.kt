package swm.calender.client.when2meet

import org.springframework.stereotype.Component
import swm.calender.core.common.time.DateTimeRange
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class When2meetParser {
    fun parse(
        url: String,
        html: String,
    ): When2meetParseResult {
        val urlFailure = validateUrl(url)
        if (urlFailure != null) {
            return urlFailure
        }

        if (html.isBlank()) {
            return When2meetParseResult.Failure(
                sourceUrl = url,
                reason = When2meetParseFailureReason.EMPTY_RESPONSE,
                detail = "When2meet response body is empty.",
            )
        }

        val timeSlots = parseTimeSlots(html)
        val availabilityEntries = parseAvailabilityEntries(html)

        if (timeSlots.isEmpty() || availabilityEntries.isEmpty()) {
            return When2meetParseResult.Failure(
                sourceUrl = url,
                reason = When2meetParseFailureReason.UNSUPPORTED_HTML,
                detail = "When2meet availability script data was not found.",
            )
        }

        val slotsByKey = timeSlots.flatMap { slot ->
            listOf(
                slot.index to slot,
                slot.startsAt.toEpochSecond() to slot,
            )
        }.toMap()
        val totalMemberCount = parseTotalMemberCount(html, availabilityEntries, slotsByKey.keys)
        val groupedAvailability = availabilityEntries
            .mapNotNull { entry -> entry.toAvailableSlot(slotsByKey) }
            .groupBy { it.slot.index }
            .mapValues { (_, availability) ->
                availability
                    .groupBy { it.personKey }
                    .mapValues { (_, personAvailability) -> personAvailability.any { it.available } }
            }

        if (groupedAvailability.isEmpty()) {
            return When2meetParseResult.Failure(
                sourceUrl = url,
                reason = When2meetParseFailureReason.NO_AVAILABILITY_SLOTS,
                detail = "No availability entries matched parsed When2meet time slots.",
            )
        }

        val slotDuration = resolveSlotDuration(timeSlots)
        val slots = timeSlots
            .sortedBy { it.startsAt }
            .map { slot ->
                val availableMemberCount = groupedAvailability[slot.index].orEmpty().values.count { it }
                When2meetAvailabilitySlot(
                    range = DateTimeRange(
                        startsAt = slot.startsAt,
                        endsAt = slot.startsAt.plus(slotDuration),
                    ),
                    availableMemberCount = availableMemberCount,
                    busyMemberCount = totalMemberCount - availableMemberCount,
                )
            }

        if (slots.isEmpty()) {
            return When2meetParseResult.Failure(
                sourceUrl = url,
                reason = When2meetParseFailureReason.NO_AVAILABILITY_SLOTS,
                detail = "No availability slots were parsed from the When2meet response.",
            )
        }

        return When2meetParseResult.Success(
            sourceUrl = url,
            slots = slots,
        )
    }

    private fun validateUrl(url: String): When2meetParseResult.Failure? {
        val uri = runCatching { URI(url.trim()) }.getOrNull()
            ?: return invalidUrl(url, "URL is not a valid URI.")
        val host = uri.host

        if (uri.scheme != ALLOWED_SCHEME) {
            return invalidUrl(url, "When2meet URL must use HTTPS.")
        }
        if (host == null || !host.equals(ALLOWED_HOST, ignoreCase = true)) {
            return invalidUrl(url, "When2meet URL host must be when2meet.com.")
        }
        if (uri.userInfo != null) {
            return invalidUrl(url, "When2meet URL must not contain user info.")
        }
        if (uri.port != -1 && uri.port != HTTPS_PORT) {
            return invalidUrl(url, "When2meet URL must use the default HTTPS port.")
        }

        return null
    }

    private fun invalidUrl(
        url: String,
        detail: String,
    ): When2meetParseResult.Failure {
        return When2meetParseResult.Failure(
            sourceUrl = url,
            reason = When2meetParseFailureReason.INVALID_URL,
            detail = detail,
        )
    }

    private fun parseTimeSlots(html: String): List<When2meetRawSlot> {
        return TIME_OF_SLOT_REGEX.findAll(html)
            .mapNotNull { match ->
                val index = match.groupValues[1].toLongOrNull()
                val epochSecond = match.groupValues[2].toLongOrNull()

                if (index == null || epochSecond == null) {
                    null
                } else {
                    When2meetRawSlot(
                        index = index,
                        startsAt = Instant.ofEpochSecond(epochSecond).atOffset(ZoneOffset.UTC),
                    )
                }
            }
            .distinctBy { it.index }
            .toList()
    }

    private fun parseAvailabilityEntries(html: String): List<When2meetAvailabilityEntry> {
        return AVAILABLE_AT_SLOT_REGEX.findAll(html)
            .mapNotNull { match ->
                val row = match.groupValues[1].toLongOrNull()
                val column = match.groupValues[2].toLongOrNull()
                val available = match.groupValues[3].toBooleanValueOrNull()

                if (row == null || column == null || available == null) {
                    null
                } else {
                    When2meetAvailabilityEntry(
                        row = row,
                        column = column,
                        available = available,
                    )
                }
            }
            .toList()
    }

    private fun parseTotalMemberCount(
        html: String,
        entries: List<When2meetAvailabilityEntry>,
        slotKeys: Set<Long>,
    ): Int {
        val peopleCount = listOf(PEOPLE_IDS_REGEX, PEOPLE_NAMES_REGEX)
            .maxOf { regex -> regex.findAll(html).map { it.groupValues[1] }.toSet().size }

        val inferredMemberCount = entries
            .flatMap { entry ->
                listOfNotNull(
                    entry.row.takeUnless { it in slotKeys },
                    entry.column.takeUnless { it in slotKeys },
                )
            }
            .toSet()
            .size

        return maxOf(peopleCount, inferredMemberCount)
    }

    private fun When2meetAvailabilityEntry.toAvailableSlot(
        slotsByKey: Map<Long, When2meetRawSlot>,
    ): When2meetSlotAvailability? {
        val slot = slotsByKey[row] ?: slotsByKey[column] ?: return null
        val personKey = if (slotsByKey.containsKey(row)) column else row

        return When2meetSlotAvailability(
            slot = slot,
            personKey = personKey,
            available = available,
        )
    }

    private fun resolveSlotDuration(slots: List<When2meetRawSlot>): Duration {
        return slots
            .map { it.startsAt }
            .sorted()
            .zipWithNext()
            .map { (startsAt, nextStartsAt) -> Duration.between(startsAt, nextStartsAt) }
            .filter { it > Duration.ZERO }
            .minOrNull()
            ?: DEFAULT_SLOT_DURATION
    }

    private fun String.toBooleanValueOrNull(): Boolean? {
        return when (trim().trim('"', '\'')) {
            "1", "true" -> true
            "0", "false" -> false
            else -> null
        }
    }

    private companion object {
        private const val ALLOWED_SCHEME = "https"
        private const val ALLOWED_HOST = "when2meet.com"
        private const val HTTPS_PORT = 443
        private val DEFAULT_SLOT_DURATION: Duration = Duration.ofMinutes(30)
        private val TIME_OF_SLOT_REGEX = Regex("""TimeOfSlot\[(\d+)]\s*=\s*['"]?(\d+)['"]?\s*;""")
        private val AVAILABLE_AT_SLOT_REGEX = Regex(
            """AvailableAtSlot\[(\d+)]\[(\d+)]\s*=\s*['"]?(1|0|true|false)['"]?\s*;""",
            RegexOption.IGNORE_CASE,
        )
        private val PEOPLE_IDS_REGEX = Regex("""PeopleIDs\[(\d+)]\s*=""")
        private val PEOPLE_NAMES_REGEX = Regex("""PeopleNames\[(\d+)]\s*=""")
    }
}

sealed interface When2meetParseResult {
    val sourceUrl: String

    data class Success(
        override val sourceUrl: String,
        val slots: List<When2meetAvailabilitySlot>,
    ) : When2meetParseResult

    data class Failure(
        override val sourceUrl: String,
        val reason: When2meetParseFailureReason,
        val detail: String,
    ) : When2meetParseResult
}

data class When2meetAvailabilitySlot(
    val range: DateTimeRange,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
) {
    init {
        require(availableMemberCount >= 0) { "availableMemberCount must be zero or positive." }
        require(busyMemberCount >= 0) { "busyMemberCount must be zero or positive." }
    }
}

enum class When2meetParseFailureReason {
    INVALID_URL,
    EMPTY_RESPONSE,
    UNSUPPORTED_HTML,
    NO_AVAILABILITY_SLOTS,
}

private data class When2meetRawSlot(
    val index: Long,
    val startsAt: OffsetDateTime,
)

private data class When2meetAvailabilityEntry(
    val row: Long,
    val column: Long,
    val available: Boolean,
)

private data class When2meetSlotAvailability(
    val slot: When2meetRawSlot,
    val personKey: Long,
    val available: Boolean,
)
