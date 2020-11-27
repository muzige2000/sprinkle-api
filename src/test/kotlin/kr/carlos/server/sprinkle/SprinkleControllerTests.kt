package kr.carlos.server.sprinkle

import com.google.gson.Gson
import kr.carlos.server.sprinkle.controller.X_ROOM_ID
import kr.carlos.server.sprinkle.controller.X_USER_ID
import kr.carlos.server.sprinkle.model.ChatMember
import kr.carlos.server.sprinkle.repository.ChatMemberRepository
import kr.carlos.server.sprinkle.repository.SprinkleRepository
import kr.carlos.server.sprinkle.repository.SprinkledCashRepository
import kr.carlos.server.sprinkle.request.RequestCreateSprinkle
import kr.carlos.server.sprinkle.response.ResponseCreateSprinkle
import kr.carlos.server.sprinkle.service.SprinkleService
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import kotlin.concurrent.thread
import kotlin.properties.Delegates
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SprinkleControllerTests(@Autowired val mockMvc: MockMvc) {
    @Autowired
    lateinit var gson: Gson

    @Autowired
    lateinit var sprinkleRepository: SprinkleRepository

    @Autowired
    lateinit var sprinkledCashRepository: SprinkledCashRepository

    @Autowired
    lateinit var chatMemberRepository: ChatMemberRepository

    @Autowired
    lateinit var sprinkleService: SprinkleService

    var roomId: String by Delegates.notNull()
    var wrongRoomId: String by Delegates.notNull()
    var ownerUserId: Long by Delegates.notNull()
    var memberUserIds: ArrayList<Long> = ArrayList()
    var wrongUserId: Long by Delegates.notNull()
    var chatMemberSize: Int by Delegates.notNull()

    @BeforeAll
    fun setup() {
        roomId = Random.nextInt(1, 10000000).toString()
        wrongRoomId = Random.nextInt(10000000, 20000000).toString()
        chatMemberSize = Random.nextInt(3, 10)
        var from: Long = 1
        val interval = 100
        for (i in 0 until chatMemberSize) {
            val userId: Long = Random.nextLong(from, from + interval)
            if (i == 0) {
                ownerUserId = userId
            } else {
                memberUserIds.add(userId)
            }
            chatMemberRepository.save(ChatMember(roomId, userId))
            from += interval
        }
        wrongUserId = Random.nextLong(from, from + interval)
    }

    @AfterAll
    fun tearDown() {
        chatMemberRepository.deleteAll()
        sprinkledCashRepository.deleteAll()
        sprinkleRepository.deleteAll()
    }

    @Nested
    inner class `뿌리기API` {
        @Test
        fun `뿌리기가 정상적으로 생성 되어야 합니다`() {
            // 뿌릴 금액, 뿌릴 인원을 요청값으로 받습니다
            val req = RequestCreateSprinkle(2000, chatMemberSize - 1)
            val result = mockMvc.perform(post("/sprinkles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId)
                    .content(gson.toJson(req)))
                    .andExpect(status().isOk)
                    .andReturn()
            val res = gson.fromJson(result.response.contentAsString, ResponseCreateSprinkle::class.java)
            // 뿌리기 요청건에 대한 고유 token 을 발급하고 응답값으로 내려줍니다
            assertNotNull(res.token)
            // token 은 3자리 문자열로 구성되며 예측이 불가능해야합니다.?
            assertEquals(3, res.token.length)
            val sprinkle = sprinkleRepository.findByRoomIdAndToken(roomId, res.token)!!
            assertNotNull(sprinkle.id)
            assertEquals(ownerUserId, sprinkle.userId)
            assertEquals(roomId, sprinkle.roomId)
            assertEquals(req.amount, sprinkle.desiredAmount)
            assertEquals(0, sprinkle.sprinkledAmount)
            assertEquals(res.token, sprinkle.token)

            // 뿌릴 금액을 인원수에 맞게 분배하여 저장합니다. (분배로직은 자유롭게 구현)
            assertEquals(req.size, sprinkle.sprinkledCashes.size)
            var totalAmountInDB: Long = 0
            for (sprinkledCash in sprinkle.sprinkledCashes) {
                totalAmountInDB += sprinkledCash.amount
            }
            assertEquals(req.amount, totalAmountInDB)
        }

        @Test
        fun `자기 혼자만 있는 채팅방의 경우, 뿌리기를 할 수 없습니다`() {
            val roomId = Random.nextInt(20000000, 30000000).toString()
            chatMemberRepository.save(ChatMember(roomId, ownerUserId))

            mockMvc.perform(post("/sprinkles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId)
                    .content(gson.toJson(RequestCreateSprinkle(2000, 1))))
                    .andExpect(status().is4xxClientError)
                    .andReturn()
        }

        @Test
        fun `채팅방인원 이상으로 뿌리기를 할 수 없습니다`() {
            val roomId = Random.nextInt(20000000, 30000000).toString()
            chatMemberRepository.save(ChatMember(roomId, ownerUserId))
            chatMemberRepository.save(ChatMember(roomId, memberUserIds.get(0)))
            chatMemberRepository.save(ChatMember(roomId, memberUserIds.get(1)))

            mockMvc.perform(post("/sprinkles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId)
                    .content(gson.toJson(RequestCreateSprinkle(2000, 3))))
                    .andExpect(status().is4xxClientError)
                    .andReturn()
        }

        @Test
        fun `뿌리기 인원수 보다 금액이 작은 경우, 뿌리기 할 수 없습니다`() {
            val roomId = Random.nextInt(20000000, 30000000).toString()
            chatMemberRepository.save(ChatMember(roomId, ownerUserId))
            chatMemberRepository.save(ChatMember(roomId, memberUserIds.get(0)))
            chatMemberRepository.save(ChatMember(roomId, memberUserIds.get(1)))

            mockMvc.perform(post("/sprinkles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId)
                    .content(gson.toJson(RequestCreateSprinkle(1, 2))))
                    .andExpect(status().is4xxClientError)
                    .andReturn()
        }
    }

    @Nested
    inner class `받기API` {
        // 뿌리기 시 발급된 token 을 요청값으로 받습니다
        // token 에 해당하는 뿌리기 건 중 아직 누구에게도 할당되지 않은 분배건 하나를 API 를 호출한 사용자에게 할당하고, 그 금액을 응답값으로 내려줍니다
        @Test
        fun `정상적인 케이스에 대해서 뿌리기 받기를 할 수 있습니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(0))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.amount", greaterThan(0)))
            val sprinkledCash = sprinkledCashRepository.findBySprinkleIdAndUserId(sprinkle.id!!, memberUserIds.get(0))
            assertThat(sprinkledCash).isNotNull
        }

        @Test
        fun `뿌리기 당 한 사용자는 한번만 받을 수 있습니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(1))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.amount", greaterThan(0)))
                    .andReturn()
            mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(1))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
        }

        @Test
        fun `자신이 뿌리기한 건은 자신이 받을 수 없습니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
        }

        @Test
        fun `뿌리기가 호출된 대화방과 동일한 대화방에 속한 사용자만이 받을 수 있습니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(0))
                    .header(X_ROOM_ID, wrongRoomId))
                    .andExpect(status().is4xxClientError)
        }

        @Test
        fun `뿌린 건은 10분간만 유효합니다, 뿌린지 10분이 지난 요청에 대해서는 받기 실패 응답이 내려가야합니다`() {
            val expiredSprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2), 0)
            mockMvc.perform(post("/sprinkles/${expiredSprinkle.token}/pick")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(0))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
        }

        @Test
        fun `동시성체크를 하며, 최종 뿌리기금액이 목표 금액과 같아야 합니다`() {
            val stopWatch = StopWatch()
            stopWatch.start()
            var counter = 0
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            val justInTimeMemberIds = memberUserIds.slice(0 until sprinkle.sprinkledCashes.size)
            val lateMemberIds = memberUserIds.slice(sprinkle.sprinkledCashes.size until memberUserIds.size)

            assertThat(sprinkle.sprinkledCashes.size).isEqualTo(justInTimeMemberIds.size)

            fun pick(userId: Long, expected: ResultMatcher) {
//                Thread.sleep(1000)
                mockMvc.perform(post("/sprinkles/${sprinkle.token}/pick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_USER_ID, userId)
                        .header(X_ROOM_ID, roomId))
                        .andExpect(expected)
                counter += 1
            }

            val threads: ArrayList<Thread> = ArrayList()
            for (userId in justInTimeMemberIds) {
                threads.add(thread {
                    pick(userId, status().isOk)
                })
            }
            for (thread in threads) {
                thread.join()
            }
            threads.clear()
            for (userId in lateMemberIds) {
                threads.add(thread {
                    pick(userId, status().is4xxClientError)
                })
            }
            for (thread in threads) {
                thread.join()
            }
            stopWatch.stop()
            assertThat(memberUserIds.size).isEqualTo(counter)

            val newSprinkle = sprinkleRepository.findByRoomIdAndToken(roomId, sprinkle.token)
            assertThat(newSprinkle?.sprinkledAmount).isEqualTo(newSprinkle?.desiredAmount)
        }
    }

    @Nested
    inner class `조회 API` {
        // 뿌리기 시 발급된 token 을 요청값으로 받습니다
        // token 에 해당하는 뿌리기 건의 현재 상태를 응답값으로 내려줍니다, 현재 상태는 다음의 정보를 포함합니다
        // 뿌린시간, 뿌린금액, 받기 완료된 금액, 받기 완료된 정보([받은 금액, 받은 사용자 아이디] 리스트])
        @Test
        fun `정상적으로 뿌리기를 조회합니다, 뿌린 사람 자신만 조회를 할 수 있습니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(get("/sprinkles/${sprinkle.token}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.token").value(sprinkle.token))
                    .andExpect(jsonPath("$.desiredAmount").value(sprinkle.desiredAmount))
                    .andExpect(jsonPath("$.sprinkledAmount").value(sprinkle.sprinkledAmount))
        }

        @Test
        fun `다른 사람의 뿌리기건이나, 유효하지 않은 token 에 대해서는 조회 실패 응답이 내려가야 합니다`() {
            val sprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2))
            mockMvc.perform(get("/sprinkles/${sprinkle.token}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(0))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
            mockMvc.perform(get("/sprinkles/wrongtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, memberUserIds.get(0))
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
        }

        @Test
        fun `뿌린 건에 대한 조회는 7일 동안 할 수 있습니다`() {
            val sevenDaysAgoSprinkle = sprinkleService.create(ownerUserId, roomId, RequestCreateSprinkle((chatMemberSize * 1000).toLong(), chatMemberSize / 2), 0)
            sevenDaysAgoSprinkle.createdAt = LocalDateTime.now().minusDays(7)
            sprinkleRepository.save(sevenDaysAgoSprinkle)
            mockMvc.perform(get("/sprinkles/${sevenDaysAgoSprinkle.token}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(X_USER_ID, ownerUserId)
                    .header(X_ROOM_ID, roomId))
                    .andExpect(status().is4xxClientError)
        }
    }
}
