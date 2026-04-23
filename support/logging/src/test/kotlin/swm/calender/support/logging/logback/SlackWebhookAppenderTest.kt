package swm.calender.support.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant

class SlackWebhookAppenderTest {
    @Test
    fun `builds a readable slack message body`() {
        val appender = TestSlackWebhookAppender().apply {
            webhookUrl = "http://test.invalid/slack"
            serviceName = "teams"
            environment = "test"
            minimumEventLevel = "ERROR"
            start()
        }

        val message = appender.renderMessage(createEvent(Level.ERROR, "boom"))

        assertThat(message).contains("[test] teams ERROR")
        assertThat(message).contains("logger=test.slack")
        assertThat(message).contains("thread=test-thread")
        assertThat(message).contains("message:\nboom")
    }

    @Test
    fun `filters out events below the configured minimum level`() {
        val appender = TestSlackWebhookAppender().apply {
            webhookUrl = "http://test.invalid/slack"
            minimumEventLevel = "ERROR"
            start()
        }

        assertThat(appender.shouldSend(Level.ERROR)).isTrue
        assertThat(appender.shouldSend(Level.WARN)).isFalse
    }

    private fun createEvent(
        level: Level,
        message: String,
    ): LoggingEvent =
        LoggingEvent().apply {
            loggerName = "test.slack"
            this.level = level
            this.message = message
            threadName = "test-thread"
            timeStamp = Instant.parse("2026-04-23T07:30:00Z").toEpochMilli()
        }

    private class TestSlackWebhookAppender : SlackWebhookAppender() {
        fun renderMessage(event: LoggingEvent): String = buildMessage(event)

        fun shouldSend(level: Level): Boolean = shouldNotify(level)

        override fun sendPayload(
            uri: URI,
            requestBody: String,
        ) {
            error("sendPayload should not be called in unit tests")
        }
    }
}
