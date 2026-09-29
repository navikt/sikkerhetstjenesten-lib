package no.nav.tilgangsmaskin.felles.rest.notifikasjon

import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.felles.notifikasjon.AbstractAuditor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SecureAuditor(logger: Logger = LoggerFactory.getLogger("secureLog")) : AbstractAuditor(logger)