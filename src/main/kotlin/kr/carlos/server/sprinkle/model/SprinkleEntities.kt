package kr.carlos.server.sprinkle.model

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

private const val TokenLength = 3
private const val ReadableDays: Long = 7

@Entity
@Table(indexes = [Index(name = "idx_room_id_and_token", columnList = "roomId,token", unique = true)])
data class Sprinkle(
        var userId: Long,
        var roomId: String,
        var desiredAmount: Long,
        var sprinkledAmount: Long = 0,
        var token: String,
        @JsonManagedReference
        @OneToMany(fetch = FetchType.EAGER, cascade = arrayOf(CascadeType.ALL), orphanRemoval = true)
        var sprinkledCashes: MutableList<SprinkledCash> = ArrayList(),
        var expiresAt: LocalDateTime,
        var createdAt: LocalDateTime,
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
) {
    constructor(userId: Long, roomId: String, desiredAmount: Long, expiresMinutes: Long) :
            this(userId, roomId, desiredAmount, 0,
                    RandomStringUtils.randomAlphanumeric(TokenLength), ArrayList(),
                    LocalDateTime.now().plusMinutes(expiresMinutes), LocalDateTime.now())

    fun isExpired() = this.expiresAt < LocalDateTime.now()
    fun isReadable() = this.createdAt > LocalDateTime.now().minusDays(ReadableDays)
    fun isOwner(userId: Long) = this.userId == userId
    fun addSprinkledCash(sprinkledCash: SprinkledCash) {
        sprinkledCashes.add(sprinkledCash)
        sprinkledCash.sprinkle = this
    }

    fun take(userId: Long): SprinkledCash? {
        for (sprinkledCash in sprinkledCashes) {
            if (sprinkledCash.userId == null) {
                sprinkledCash.userId = userId
                return sprinkledCash
            }
        }
        return null
    }

    fun isPickedUser(userId: Long): Boolean {
        for (sprinkledCash in sprinkledCashes) {
            if (sprinkledCash.userId == userId) {
                return true
            }
        }
        return false
    }
}

@Entity
data class SprinkledCash(
        @JsonBackReference
        @ManyToOne(fetch = FetchType.LAZY)
        var sprinkle: Sprinkle,
        val amount: Long,
        var userId: Long? = null,
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

@Entity
@Table(indexes = arrayOf(Index(name = "idx_room_id_and_user_id", columnList = "roomId,userId", unique = true)))
data class ChatMember(
        var roomId: String,
        var userId: Long,
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)


