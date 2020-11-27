package kr.carlos.server.sprinkle.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class InvalidParameterException : RuntimeException("Invalid parameter.")

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class BadRequestException : RuntimeException("Bad request.")

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class NotFoundException : RuntimeException("Not found.")

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class ExpiredException : RuntimeException("Expired.")

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class NoMoreSprinkledCashException : RuntimeException("No more sprinkled cash.")

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class AlreadyPickedUserException() : RuntimeException("Already picked user.")

