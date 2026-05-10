package swm.calender.core.team.service

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RandomTeamInviteCodeGenerator : TeamInviteCodeGenerator {
    override fun generate(): String {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(12)
            .uppercase()
    }
}
