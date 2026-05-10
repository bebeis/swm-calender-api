package swm.calender.client.example

import org.springframework.stereotype.Component
import swm.calender.client.example.model.ExampleClientResult

@Component
class ExampleClient internal constructor(
    private val exampleApi: ExampleApi,
) {
    fun example(exampleParameter: String): ExampleClientResult {
        val request = ExampleRequestDto(exampleParameter)
        return exampleApi.example(request).toResult()
    }
}
