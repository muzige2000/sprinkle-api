package kr.carlos.server.sprinkle.controller

import kr.carlos.server.sprinkle.model.Sprinkle
import kr.carlos.server.sprinkle.request.RequestCreateSprinkle
import kr.carlos.server.sprinkle.response.ResponseCreateSprinkle
import kr.carlos.server.sprinkle.response.ResponsePickSprinkle
import kr.carlos.server.sprinkle.service.SprinkleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


const val X_USER_ID = "X-USER-ID"
const val X_ROOM_ID = "X-ROOM-ID"


@RestController
@RequestMapping("/sprinkles")
class SprinklingController(private val sprinkleService: SprinkleService) {

    @PostMapping
    fun createSprinkle(@RequestHeader(X_USER_ID) userId: Long,
                       @RequestHeader(X_ROOM_ID) roomId: String,
                       @RequestBody params: RequestCreateSprinkle): ResponseEntity<ResponseCreateSprinkle> {
        return ResponseEntity.ok(ResponseCreateSprinkle(sprinkleService.create(userId, roomId, params).token))
    }


    @GetMapping("/{token}")
    fun getSprinkleByToken(@RequestHeader(X_USER_ID) userId: Long,
                           @RequestHeader(X_ROOM_ID) roomId: String,
                           @PathVariable token: String): ResponseEntity<Sprinkle> {
        return ResponseEntity.ok(sprinkleService.get(userId, roomId, token))
    }


    @PostMapping("/{token}/pick")
    fun pickSprinkle(@RequestHeader(X_USER_ID) userId: Long,
                     @RequestHeader(X_ROOM_ID) roomId: String,
                     @PathVariable token: String): ResponseEntity<ResponsePickSprinkle> {
        return ResponseEntity.ok(ResponsePickSprinkle(sprinkleService.pick(userId, roomId, token)))
    }

}
