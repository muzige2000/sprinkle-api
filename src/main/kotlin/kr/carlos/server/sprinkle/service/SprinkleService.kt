package kr.carlos.server.sprinkle.service

import kr.carlos.server.sprinkle.exception.*
import kr.carlos.server.sprinkle.model.Sprinkle
import kr.carlos.server.sprinkle.model.SprinkledCash
import kr.carlos.server.sprinkle.repository.ChatMemberRepository
import kr.carlos.server.sprinkle.repository.SprinkleRepository
import kr.carlos.server.sprinkle.request.RequestCreateSprinkle
import org.springframework.stereotype.Service
import javax.transaction.Transactional
import kotlin.random.Random
import java.util.Arrays

private const val ExpiresMinutes: Long = 10

@Service
class SprinkleService(val sprinkleRepository: SprinkleRepository, val chatMemberRepository: ChatMemberRepository) {

    @Transactional
    fun create(userId: Long, roomId: String, newSprinkle: RequestCreateSprinkle, expiresMinutes: Long = ExpiresMinutes): Sprinkle {
        val memberCount = chatMemberRepository.countByRoomId(roomId)
        if (newSprinkle.amount < newSprinkle.size ||
                memberCount < 2 || memberCount <= newSprinkle.size) throw BadRequestException()

        val sprinkle = Sprinkle(userId, roomId, newSprinkle.amount, expiresMinutes)
        val sprinkledAmounts = sprinkleAmountBySize(newSprinkle.amount, newSprinkle.size)
        for (sprinkledAmount in sprinkledAmounts) {
            sprinkle.addSprinkledCash(SprinkledCash(sprinkle, sprinkledAmount, null))
        }
        return sprinkleRepository.save(sprinkle)
    }

    fun get(userId: Long, roomId: String, token: String): Sprinkle {
        val sprinkle = sprinkleRepository.findByRoomIdAndToken(roomId, token) ?: throw NotFoundException()
        if (!sprinkle.isOwner(userId)) throw BadRequestException()
        if (!sprinkle.isReadable()) throw ExpiredException()
        return sprinkle
    }

    @Transactional
    fun pick(userId: Long, roomId: String, token: String): Long {
        val sprinkle = sprinkleRepository.findByRoomIdAndTokenForUpdate(roomId, token) ?: throw NotFoundException()
        if (sprinkle.isOwner(userId)) throw BadRequestException()
        if (sprinkle.isExpired()) throw ExpiredException()
        if (sprinkle.isPickedUser(userId)) throw AlreadyPickedUserException()
        val sprinkledCash = sprinkle.take(userId) ?: throw NoMoreSprinkledCashException()
        sprinkle.sprinkledAmount += sprinkledCash.amount
        sprinkleRepository.save(sprinkle)
        return sprinkledCash.amount
    }

    private fun sprinkleAmountBySize(amount: Long, size: Int): LongArray {
        val vals = LongArray(size)
        if (amount < size) return vals

        var sum: Long = amount
        sum -= size

        for (i in 0 until size - 1) {
            vals[i] = Random.nextLong(1, sum)
        }
        vals[size - 1] = sum

        Arrays.sort(vals)
        for (i in size - 1 downTo 1) {
            vals[i] -= vals[i - 1]
        }
        for (i in 0 until size) {
            ++vals[i]
        }
        return vals
    }
}

