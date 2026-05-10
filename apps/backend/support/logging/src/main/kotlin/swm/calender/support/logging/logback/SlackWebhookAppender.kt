package swm.calender.support.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

open class SlackWebhookAppender : AppenderBase<ILoggingEvent>() {
    private val objectMapper = jacksonObjectMapper()
    private val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build()

    var webhookUrl: String? = null
    var serviceName: String = "teams"
    var environment: String = "unknown"
    var minimumEventLevel: String = Level.WARN.levelStr
    var maxMessageLength: Int = 1_000
    var maxStackTraceLength: Int = 2_500

    private var webhookUri: URI? = null
    private var minimumLevel: Level = Level.WARN

    override fun start() {
        minimumLevel = parseLevel(minimumEventLevel)
        webhookUri = parseWebhookUri(webhookUrl)
        super.start()
    }

    override fun append(eventObject: ILoggingEvent) {
        val uri = webhookUri ?: return
        if (!shouldNotify(eventObject.level)) {
            return
        }

        val requestBody =
            runCatching {
                objectMapper.writeValueAsString(SlackWebhookPayload(text = buildMessage(eventObject)))
            }.getOrElse { exception ->
                addError("Failed to serialize Slack webhook payload", exception)
                return
            }

        runCatching {
            sendPayload(uri, requestBody)
        }.onFailure { exception ->
            addError("Failed to post log event to Slack webhook", exception)
        }
    }

    protected open fun sendPayload(
        uri: URI,
        requestBody: String,
    ) {
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Slack webhook returned status=${response.statusCode()} body=${response.body()}")
        }
    }

    protected fun shouldNotify(level: Level): Boolean = level.isGreaterOrEqual(minimumLevel)

    private fun parseLevel(value: String): Level = runCatching {
        Level.valueOf(value.uppercase())
    }.getOrElse {
        addWarn("Invalid minimumEventLevel=$value. Falling back to WARN.")
        Level.WARN
    }

    private fun parseWebhookUri(value: String?): URI? {
        val normalized = value?.trim().orEmpty()
        if (normalized.isBlank()) {
            addInfo("SLACK_ERROR_WEBHOOK_URL is empty. Slack error notifications are disabled.")
            return null
        }

        return runCatching {
            URI.create(normalized)
        }.getOrElse { exception ->
            addError("Invalid Slack webhook URL", exception)
            null
        }
    }

    protected open fun buildMessage(event: ILoggingEvent): String {
        val mdcPropertyMap = runCatching { event.mdcPropertyMap ?: emptyMap() }.getOrDefault(emptyMap())
        val formattedMessage = event.formattedMessage?.ifBlank { "(empty message)" } ?: "(empty message)"
        val loggerName = event.loggerName ?: "unknown"
        val threadName = event.threadName ?: "unknown"
        val message = truncate(sanitize(formattedMessage), maxMessageLength)
        val stackTrace =
            event.throwableProxy
                ?.let(ThrowableProxyUtil::asString)
                ?.let(::sanitize)
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { truncate(it, maxStackTraceLength) }

        return buildString {
            append("[")
            append(environment.ifBlank { "unknown" })
            append("] ")
            append(serviceName.ifBlank { "teams" })
            append(" ")
            append(event.level.levelStr)
            append('\n')
            append("timestamp=").append(Instant.ofEpochMilli(event.timeStamp)).append('\n')
            append("logger=").append(loggerName).append('\n')
            append("thread=").append(threadName).append('\n')
            mdcPropertyMap["traceId"]?.takeIf(String::isNotBlank)?.let {
                append("traceId=").append(it).append('\n')
            }
            mdcPropertyMap["spanId"]?.takeIf(String::isNotBlank)?.let {
                append("spanId=").append(it).append('\n')
            }
            append("message:\n")
            append(message)

            if (stackTrace != null) {
                append("\nstacktrace:\n")
                append(stackTrace)
            }
        }
    }

    private fun sanitize(value: String): String = value
        .replace("\u0000", "")
        .replace("\r\n", "\n")

    private fun truncate(value: String, maxLength: Int): String {
        if (maxLength <= 0 || value.length <= maxLength) {
            return value
        }

        val suffix = "\n...(truncated)"
        if (maxLength <= suffix.length) {
            return value.take(maxLength)
        }

        return value.take(maxLength - suffix.length) + suffix
    }

    private data class SlackWebhookPayload(
        val text: String,
    )
}
