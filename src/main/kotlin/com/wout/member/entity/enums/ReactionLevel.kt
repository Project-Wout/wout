package com.wout.member.entity.enums

/**
 * packageName    : com.wout.member.entity.enums
 * fileName       : ReactionLevel
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */
enum class ReactionLevel(val code: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    companion object {
        fun from(code: String?): ReactionLevel? =
            ReactionLevel.entries.find { it.code == code }
    }
}