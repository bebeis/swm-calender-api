package swm.calender.core.api.controller.v1

import swm.calender.core.api.controller.v1.request.ExampleRequestDto
import swm.calender.core.api.controller.v1.response.ExampleItemResponseDto
import swm.calender.core.api.controller.v1.response.ExampleResponseDto
import swm.calender.core.domain.ExampleData
import swm.calender.core.domain.ExampleService
import swm.calender.core.support.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
class ExampleController(
    private val exampleService: ExampleService,
) {
    @GetMapping("/get/{exampleValue}")
    fun exampleGet(
        @PathVariable exampleValue: String,
        @RequestParam exampleParam: String,
    ): ApiResponse<ExampleResponseDto> {
        val result = exampleService.processExample(ExampleData(exampleValue, exampleParam))
        return ApiResponse.success(
            ExampleResponseDto(
                result = result.data,
                date = LocalDate.now(),
                datetime = LocalDateTime.now(),
                items = ExampleItemResponseDto.build(),
            ),
        )
    }

    @PostMapping("/post")
    fun examplePost(
        @RequestBody request: ExampleRequestDto,
    ): ApiResponse<ExampleResponseDto> {
        val result = exampleService.processExample(request.toExampleData())
        return ApiResponse.success(
            ExampleResponseDto(
                result = result.data,
                date = LocalDate.now(),
                datetime = LocalDateTime.now(),
                items = ExampleItemResponseDto.build(),
            ),
        )
    }
}
