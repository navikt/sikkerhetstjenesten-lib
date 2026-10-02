package no.nav.sikkerhetstjenesten.felles.notifikasjon

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Fallback

@Fallback
class LocalAuditor(logger: Logger = LoggerFactory.getLogger(LocalAuditor::class.java.simpleName)) : AbstractAuditor(logger)