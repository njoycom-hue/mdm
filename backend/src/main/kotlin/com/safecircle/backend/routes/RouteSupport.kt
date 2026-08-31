package com.safecircle.backend.routes

import com.safecircle.backend.db.Pairings
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/** GUARDIAN 라우트 전반에서 공유하는 페어링 소유권 확인. */
fun isPaired(guardianId: UUID, wardId: UUID): Boolean = transaction {
    Pairings.select { (Pairings.guardianId eq guardianId) and (Pairings.wardId eq wardId) }.any()
}
