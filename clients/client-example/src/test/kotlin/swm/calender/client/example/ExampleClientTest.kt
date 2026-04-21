package swm.calender.client.example

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExampleClientTest {
    @Test
    fun returnsMappedResult() {
        val exampleApi = mockk<ExampleApi>()
        every { exampleApi.example(any()) } returns ExampleResponseDto("HELLO")

        val result = ExampleClient(exampleApi).example("PING")

        assertThat(result.exampleResult).isEqualTo("HELLO")
    }
}
