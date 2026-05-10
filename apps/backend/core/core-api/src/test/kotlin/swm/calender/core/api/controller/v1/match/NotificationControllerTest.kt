package swm.calender.core.api.controller.v1.match

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import swm.calender.core.api.controller.v1.match.service.NotificationApiFacade
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.NotificationType
import swm.calender.match.service.NotificationService
import java.time.Instant
import swm.calender.match.service.response.NotificationResponse as NotificationServiceResponse

class NotificationControllerTest : FunSpec() {
    private val restDocumentation = ManualRestDocumentation()

    private lateinit var notificationService: NotificationService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            notificationService = mockk()

            val controller = NotificationController(
                notificationApiFacade = NotificationApiFacade(notificationService),
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

        test("list notifications returns team-scoped notifications and documents endpoint") {
            // given
            every { notificationService.listNotifications(UserId(1L)) } returns listOf(
                NotificationServiceResponse(
                    notificationId = 51L,
                    type = NotificationType.REQUEST_ACCEPTED,
                    message = "Beta request accepted.",
                    read = false,
                    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                ),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get("/api/v1/notifications")
                    .principal(authenticatedUser()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[0].notificationId").value(51L))
                .andExpect(jsonPath("$.data.items[0].read").value(false))
                .andDo(
                    document(
                        "notifications-list",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.items").type(JsonFieldType.ARRAY)
                                .description("Team notifications"),
                            fieldWithPath("data.items[].notificationId").type(JsonFieldType.NUMBER)
                                .description("Notification id"),
                            fieldWithPath("data.items[].type").type(JsonFieldType.STRING)
                                .description("Notification type"),
                            fieldWithPath("data.items[].message").type(JsonFieldType.STRING)
                                .description("Notification message"),
                            fieldWithPath("data.items[].read").type(JsonFieldType.BOOLEAN)
                                .description("Read flag"),
                            fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING)
                                .description("Created time"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
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
