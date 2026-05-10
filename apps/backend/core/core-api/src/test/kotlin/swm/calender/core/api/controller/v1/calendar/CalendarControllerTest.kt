package swm.calender.core.api.controller.v1.calendar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import swm.calender.calendar.service.CalendarService
import swm.calender.calendar.service.response.CalendarAvailabilitySlotResponse
import swm.calender.calendar.service.response.CalendarMentoringScheduleBulkPushResponse
import swm.calender.calendar.service.response.CalendarUnifiedAvailabilityResponse
import swm.calender.calendar.service.response.CalendarWhen2meetLinkResponse
import swm.calender.core.api.controller.v1.calendar.request.When2meetLinkRequest
import swm.calender.core.api.controller.v1.calendar.service.CalendarApiFacade
import swm.calender.core.api.controller.v1.calendar.service.CalendarRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.When2meetLinkStatus
import java.time.Instant
import java.time.OffsetDateTime

class CalendarControllerTest : FunSpec() {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var calendarService: CalendarService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            calendarService = mockk()

            val controller = CalendarController(
                calendarApiFacade = CalendarApiFacade(calendarService),
                calendarRequestValidator = CalendarRequestValidator(validatorFactory.validator),
            )

            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .apply<StandaloneMockMvcBuilder>(
                    MockMvcRestDocumentation.documentationConfiguration(restDocumentation),
                )
                .build()
        }

        afterTest {
            restDocumentation.afterTest()
        }

        afterSpec {
            validatorFactory.close()
        }

        test("bulk push mentoring schedules returns counts and documents the endpoint") {
            // given
            every { calendarService.bulkPushMentoringSchedules(any()) } returns CalendarMentoringScheduleBulkPushResponse(
                createdCount = 1,
                skippedDuplicateCount = 1,
            )

            // when & then
            mockMvc.perform(
                post("/api/v1/calendar/mentoring-schedules:bulk-push")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "schedules": [
                            {
                              "externalSourceId": "mentor-1",
                              "title": "Weekly mentoring",
                              "startsAt": "2026-05-11T10:00:00+09:00",
                              "endsAt": "2026-05-11T11:00:00+09:00",
                              "location": "Room A",
                              "description": "Roadmap review"
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.skippedDuplicateCount").value(1))
                .andDo(
                    document(
                        "calendar-mentoring-schedules-bulk-push",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("schedules").type(JsonFieldType.ARRAY)
                                .description("Mentoring schedules to push"),
                            fieldWithPath("schedules[].externalSourceId").type(JsonFieldType.STRING)
                                .description("Team-scoped external schedule id"),
                            fieldWithPath("schedules[].title").type(JsonFieldType.STRING)
                                .description("Schedule title"),
                            fieldWithPath("schedules[].startsAt").type(JsonFieldType.STRING)
                                .description("Schedule start date-time"),
                            fieldWithPath("schedules[].endsAt").type(JsonFieldType.STRING)
                                .description("Schedule end date-time"),
                            fieldWithPath("schedules[].location").type(JsonFieldType.STRING)
                                .description("Schedule location").optional(),
                            fieldWithPath("schedules[].description").type(JsonFieldType.STRING)
                                .description("Schedule description").optional(),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.createdCount").type(JsonFieldType.NUMBER)
                                .description("Created schedule count"),
                            fieldWithPath("data.skippedDuplicateCount").type(JsonFieldType.NUMBER)
                                .description("Skipped duplicate count"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("put when2meet link returns parser status and documents the endpoint") {
            // given
            every { calendarService.registerWhen2meetLink(any()) } returns CalendarWhen2meetLinkResponse(
                url = "https://when2meet.com/?123",
                status = When2meetLinkStatus.PENDING,
                failureReason = null,
            )

            // when & then
            mockMvc.perform(
                put("/api/v1/calendar/when2meet-link")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            When2meetLinkRequest(url = "https://when2meet.com/?123"),
                        ),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.url").value("https://when2meet.com/?123"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(
                    document(
                        "calendar-when2meet-link-put",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("url").type(JsonFieldType.STRING).description("When2meet URL"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.url").type(JsonFieldType.STRING).description("When2meet URL"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description("When2meet parsing status"),
                            fieldWithPath("data.failureReason").type(JsonFieldType.VARIES)
                                .description("Failure reason when parsing failed").optional(),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("get unified availability returns slots and documents query parameters") {
            // given
            every { calendarService.getUnifiedAvailability(any()) } returns CalendarUnifiedAvailabilityResponse(
                slots = listOf(
                    CalendarAvailabilitySlotResponse(
                        startsAt = OffsetDateTime.parse("2026-05-11T10:00:00+09:00"),
                        endsAt = OffsetDateTime.parse("2026-05-11T10:30:00+09:00"),
                        availableMemberCount = 2,
                        busyMemberCount = 1,
                    ),
                ),
                generatedAt = Instant.parse("2026-05-10T00:00:00Z"),
            )

            // when & then
            mockMvc.perform(
                get("/api/v1/calendar/availability")
                    .principal(authenticatedUser())
                    .queryParam("startsAt", "2026-05-11T09:00:00+09:00")
                    .queryParam("endsAt", "2026-05-11T12:00:00+09:00")
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.slots.length()").value(1))
                .andExpect(jsonPath("$.data.slots[0].availableMemberCount").value(2))
                .andDo(
                    document(
                        "calendar-availability-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("startsAt").description("Query range start date-time"),
                            parameterWithName("endsAt").description("Query range end date-time"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.slots").type(JsonFieldType.ARRAY)
                                .description("Unified availability slots"),
                            fieldWithPath("data.slots[].startsAt").type(JsonFieldType.STRING)
                                .description("Slot start date-time"),
                            fieldWithPath("data.slots[].endsAt").type(JsonFieldType.STRING)
                                .description("Slot end date-time"),
                            fieldWithPath("data.slots[].availableMemberCount").type(JsonFieldType.NUMBER)
                                .description("Available member count"),
                            fieldWithPath("data.slots[].busyMemberCount").type(JsonFieldType.NUMBER)
                                .description("Busy member count"),
                            fieldWithPath("data.generatedAt").type(JsonFieldType.STRING)
                                .description("Response generation timestamp"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("bulk push rejects invalid request bodies with a 400 envelope") {
            // when
            val response = mockMvc.perform(
                post("/api/v1/calendar/mentoring-schedules:bulk-push")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"schedules":[]}"""),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.error.data.reason").exists())
                .andReturn()

            // then
            response.response.contentAsString.contains("schedules must not be empty") shouldBe true
        }
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            AuthenticatedUser(
                userId = UserId(1L),
                email = "owner@swm.app",
                name = "Owner",
            ),
            null,
            emptyList(),
        )
    }
}
