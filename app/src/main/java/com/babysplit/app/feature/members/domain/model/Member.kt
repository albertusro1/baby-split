package com.babysplit.app.feature.members.domain.model

enum class MemberType {
    HOST,            // App owner / host
    OFFLINE_TAGGED,  // Tagged member (name-only or name+phone)
    GMAIL_INVITED    // Invited via Gmail (auto-receives receipt upon trip finish)
}

data class Member(
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val memberType: MemberType = MemberType.OFFLINE_TAGGED,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarColorHex: String = "#3F51B5"
)
