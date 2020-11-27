package kr.carlos.server.sprinkle.repository

import kr.carlos.server.sprinkle.model.ChatMember
import kr.carlos.server.sprinkle.model.Sprinkle
import kr.carlos.server.sprinkle.model.SprinkledCash
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import javax.persistence.LockModeType

interface SprinkleRepository : CrudRepository<Sprinkle, Long> {
    fun findByRoomIdAndToken(roomId: String, token: String): Sprinkle?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sprinkle s where s.roomId = :roomId and s.token = :token")
    fun findByRoomIdAndTokenForUpdate(roomId: String, token: String): Sprinkle?

}

interface SprinkledCashRepository : CrudRepository<SprinkledCash, Long> {
    fun findBySprinkleIdAndUserId(sprinkleId: Long, userId: Long): SprinkledCash?
}

interface ChatMemberRepository : CrudRepository<ChatMember, Long> {
    fun findByRoomIdAndUserId(roomId: String, userId: Long): ChatMember?
    fun countByRoomId(roomId: String): Long
}

