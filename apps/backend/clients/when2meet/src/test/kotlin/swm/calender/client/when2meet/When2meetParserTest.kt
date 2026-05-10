package swm.calender.client.when2meet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import swm.calender.core.common.time.DateTimeRange
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class When2meetParserTest :
    FunSpec({
        val parser = When2meetParser()

        test("parse returns normalized availability slots from When2meet script data") {
            // given
            val sourceUrl = "https://when2meet.com/?123456-test"

            // when
            val result = parser.parse(sourceUrl, availabilityFixture()) as When2meetParseResult.Success

            // then
            result.sourceUrl shouldBe sourceUrl
            result.slots shouldHaveSize 2
            result.slots[0] shouldBe When2meetAvailabilitySlot(
                range = DateTimeRange(
                    startsAt = OffsetDateTime.ofInstant(FIRST_SLOT_START, ZoneOffset.UTC),
                    endsAt = OffsetDateTime.ofInstant(SECOND_SLOT_START, ZoneOffset.UTC),
                ),
                availableMemberCount = 2,
                busyMemberCount = 1,
            )
            result.slots[1] shouldBe When2meetAvailabilitySlot(
                range = DateTimeRange(
                    startsAt = OffsetDateTime.ofInstant(SECOND_SLOT_START, ZoneOffset.UTC),
                    endsAt = OffsetDateTime.ofInstant(THIRD_SLOT_START, ZoneOffset.UTC),
                ),
                availableMemberCount = 1,
                busyMemberCount = 2,
            )
        }

        test("parse rejects URLs outside exact HTTPS when2meet.com") {
            // given
            val invalidUrls = listOf(
                "http://when2meet.com/?event=1",
                "https://www.when2meet.com/?event=1",
                "https://when2meet.com.evil.test/?event=1",
                "https://127.0.0.1/?event=1",
                "https://when2meet.com:8443/?event=1",
                "not-a-url",
            )

            invalidUrls.forEach { invalidUrl ->
                // when
                val result = parser.parse(invalidUrl, availabilityFixture()) as When2meetParseResult.Failure

                // then
                result.sourceUrl shouldBe invalidUrl
                result.reason shouldBe When2meetParseFailureReason.INVALID_URL
            }
        }

        test("parse returns a typed failure when the HTML structure cannot be parsed") {
            // given
            val html = """
                <html>
                    <body>When2meet changed this page structure.</body>
                </html>
            """.trimIndent()

            // when
            val result = parser.parse("https://when2meet.com/?changed", html) as When2meetParseResult.Failure

            // then
            result.reason shouldBe When2meetParseFailureReason.UNSUPPORTED_HTML
        }
    }) {
    companion object {
        private const val SLOT_SECONDS = 1_800L
        private val FIRST_SLOT_START: Instant = Instant.parse("2026-05-10T01:00:00Z")
        private val SECOND_SLOT_START: Instant = FIRST_SLOT_START.plusSeconds(SLOT_SECONDS)
        private val THIRD_SLOT_START: Instant = SECOND_SLOT_START.plusSeconds(SLOT_SECONDS)

        private fun availabilityFixture(): String {
            return """
                <html>
                    <head><title>When2meet</title></head>
                    <body>
                        <script>
                            var PeopleIDs = new Array();
                            PeopleIDs[0] = 101;
                            PeopleIDs[1] = 102;
                            PeopleIDs[2] = 103;
                            var PeopleNames = new Array();
                            PeopleNames[0] = "Min";
                            PeopleNames[1] = "Ari";
                            PeopleNames[2] = "Jun";
                            var TimeOfSlot = new Array();
                            TimeOfSlot[0] = ${FIRST_SLOT_START.epochSecond};
                            TimeOfSlot[1] = ${SECOND_SLOT_START.epochSecond};
                            var AvailableAtSlot = new Array();
                            AvailableAtSlot[0] = new Array();
                            AvailableAtSlot[0][0] = 1;
                            AvailableAtSlot[0][1] = 1;
                            AvailableAtSlot[0][2] = 0;
                            AvailableAtSlot[1] = new Array();
                            AvailableAtSlot[1][0] = 0;
                            AvailableAtSlot[1][1] = 0;
                            AvailableAtSlot[1][2] = 1;
                        </script>
                    </body>
                </html>
            """.trimIndent()
        }
    }
}
