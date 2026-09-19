package br.com.erudio.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/test/v1")
class TestLogController {

    private val logger = Logger.getLogger(TestLogController::class.java.name)

    @GetMapping
    fun testLog(): String {
        logger.fine("This is an DEBUG log")
        logger.info("This is an INFO log")
        logger.warning("This is an WARN log")
        logger.severe("This is an ERROR log")
        return "Logs generated successfully!"
    }
}
