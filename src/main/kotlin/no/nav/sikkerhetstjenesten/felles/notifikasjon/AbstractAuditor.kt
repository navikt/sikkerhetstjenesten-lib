package no.nav.sikkerhetstjenesten.felles.notifikasjon

import org.slf4j.Logger

abstract class AbstractAuditor(protected val logger: Logger) : Auditor {
    override fun info(message: String, t: Throwable?) = t?.let { logger.info(message, it) } ?: logger.info(message)
}