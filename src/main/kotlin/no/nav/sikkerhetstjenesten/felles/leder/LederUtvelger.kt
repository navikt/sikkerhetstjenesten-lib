package no.nav.sikkerhetstjenesten.felles.leder

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

class LederUtvelger(private val sseUtvelger: SSELederUtvelger,
                    private val pollendeUtvelger: PollendeLederUtvelger,
                    private val varsler: LederVarsler) {


    @EventListener(ApplicationReadyEvent::class)
    fun klar() {
        sseUtvelger.subscribe { varsler.varsle(it.name) }
        varsler.varsle(pollendeUtvelger.poll()?.name)
    }
}