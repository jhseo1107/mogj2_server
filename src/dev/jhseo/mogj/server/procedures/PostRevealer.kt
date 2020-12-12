package dev.jhseo.mogj.server.procedures

import dev.jhseo.mogj.server.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjuster

fun revealDateTime(user: User): LocalDateTime? { // Should return revealTime of Yesterday -> updated_at < yesterday is allowed
    val currentTime = LocalDateTime.now()
    if(user.revealTime != null) {
        if(user.revealTime < currentTime.toLocalTime()) {
            return LocalDateTime.of(LocalDate.now(), user.revealTime)
        } else {
            return LocalDateTime.of(LocalDate.now().minusDays(1L), user.revealTime)
        }
    }
    return null
}
