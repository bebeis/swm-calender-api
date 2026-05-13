package swm.calender.client.gemini

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "swm.clients.gemini")
data class GeminiDuplicateAnalysisProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val model: String = "gemini-2.5-flash",
    val temperature: Double = 0.0,
    val maxOutputTokens: Int = 4096,
    val maxReleasedServices: Int = 120,
    val maxCandidateIdeas: Int = 120,
    val maxMatches: Int = 20,
)
