package kr.carlos.server.sprinkle

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class SprinklingApplication

fun main(args: Array<String>) {
    runApplication<SprinklingApplication>(*args)
}
