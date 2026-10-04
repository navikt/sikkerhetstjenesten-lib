package no.nav.sikkerhetstjenesten.felles.notifikasjon

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger

class AuditorTest : BehaviorSpec({
    val logger = mockk<Logger>(relaxed = true)

    Given("LocalAuditor") {
        val auditor = LocalAuditor(logger)
        beforeEach {
            clearMocks(logger)
        }

        When("info kalles med melding") {
            Then("logger melding via klassens logger") {
                auditor.info("test melding")
                verify { logger.info("test melding") }
            }
        }

        When("info kalles med melding og throwable") {
            Then("logger melding med throwable") {
                val throwable = RuntimeException("feil")
                auditor.info("test melding", throwable)
                verify { logger.info("test melding", throwable) }
            }
        }
    }

    Given("SecureAuditor") {
        val auditor = SecureAuditor(logger)
        beforeEach {
            clearMocks(logger)
        }

        When("info kalles med sensitiv melding") {
            Then("logger melding via secureLog-loggeren") {
                auditor.info("sensitiv melding")
                verify { logger.info("sensitiv melding") }
            }
        }

        When("info kalles med sensitiv melding og throwable") {
            Then("logger melding med throwable") {
                val throwable = RuntimeException("feil")
                auditor.info("sensitiv melding", throwable)
                verify { logger.info("sensitiv melding", throwable) }
            }
        }
    }
})
