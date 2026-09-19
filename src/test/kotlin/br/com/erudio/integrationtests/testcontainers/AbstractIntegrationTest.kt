package br.com.erudio.integrationtests.testcontainers

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.lifecycle.Startables
import java.util.stream.Stream

@Suppress("DEPRECATION")
@ContextConfiguration(initializers = [AbstractIntegrationTest.Initializer::class])
open class AbstractIntegrationTest {

    protected fun greenMail(): GreenMail = Initializer.smtp

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            startContainers()
            startSmtp()

            val environment = applicationContext.environment
            val testcontainers = MapPropertySource(
                "testcontainers", createConnectionConfiguration()
            )
            environment.propertySources.addFirst(testcontainers)
        }

        companion object {

            const val SMTP_USERNAME = "sender@erudio.test"
            const val SMTP_PASSWORD = "secret"

            private val mysql: MySQLContainer<*> = MySQLContainer("mysql:9.1.0")

            val smtp: GreenMail = GreenMail(ServerSetupTest.SMTP.dynamicPort())

            private fun startContainers() {
                Startables.deepStart(Stream.of(mysql)).join()
            }

            @Synchronized
            private fun startSmtp() {
                if (smtp.isRunning) return
                smtp.start()
                smtp.setUser(SMTP_USERNAME, SMTP_PASSWORD)
                Runtime.getRuntime().addShutdownHook(Thread { smtp.stop() })
            }

            private fun createConnectionConfiguration(): Map<String, Any> {
                return mapOf(
                    "spring.datasource.url" to mysql.jdbcUrl,
                    "spring.datasource.username" to mysql.username,
                    "spring.datasource.password" to mysql.password,
                    "spring.mail.host" to "localhost",
                    "spring.mail.port" to smtp.smtp.port.toString(),
                    "spring.mail.username" to SMTP_USERNAME,
                    "spring.mail.password" to SMTP_PASSWORD,
                    "spring.mail.properties.mail.smtp.auth" to "true",
                    "spring.mail.properties.mail.smtp.starttls.enable" to "false",
                    "spring.mail.properties.mail.smtp.starttls.required" to "false"
                )
            }
        }
    }
}
