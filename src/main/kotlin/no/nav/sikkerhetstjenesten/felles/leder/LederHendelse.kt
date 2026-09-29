package no.nav.sikkerhetstjenesten.felles.leder

import org.springframework.context.ApplicationEvent

class LederHendelse(source: Any, val leder: String) : ApplicationEvent(source)