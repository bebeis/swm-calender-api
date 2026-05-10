package swm.calender.core.team.exception

enum class TeamErrorMessage(
    val message: String,
) {
    TEAM_NAME_REQUIRED("Team name is required."),
    INVITE_CODE_REQUIRED("Invite code is required."),
    TEAM_ACTIVE_OWNER_REQUIRED("Team must have at least one active OWNER."),
    TEAM_MEMBER_NAME_REQUIRED("Team member name is required."),
    TEAM_MEMBER_EMAIL_REQUIRED("Team member email is required."),
    TEAM_MEMBER_ALREADY_EXISTS("User is already an active member of this team."),
    TEAM_OWNER_REQUIRED("Only an active OWNER can perform this action."),
    TEAM_MEMBER_REQUIRED("Only an active team member can perform this action."),
    TEAM_MEMBER_NOT_FOUND("Team member not found."),
    TEAM_MEMBER_INACTIVE("Inactive team members cannot be changed."),
    TEAM_MEMBER_HISTORY_ROLE_REQUIRED("Team member history role state is invalid."),
    TEAM_ALREADY_EXISTS_FOR_USER("User already belongs to an active team."),
    TEAM_NOT_FOUND("Team not found."),
    INVALID_INVITE_CODE("Invite code is invalid."),
    TEAM_NOT_PERSISTED("Team must be persisted before this operation result can be exposed."),
}
