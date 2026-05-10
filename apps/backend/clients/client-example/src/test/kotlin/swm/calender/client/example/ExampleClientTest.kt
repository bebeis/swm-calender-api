package swm.calender.client.example

import feign.RetryableException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import swm.calender.client.ClientExampleContextTest

class ExampleClientTest(
    private val exampleClient: ExampleClient,
) : ClientExampleContextTest() {
    @Test
    fun shouldBeThrownExceptionWhenExample() {
        try {
            exampleClient.example("HELLO!")
        } catch (e: Exception) {
            Assertions.assertThat(e).isExactlyInstanceOf(RetryableException::class.java)
        }
    }
}
