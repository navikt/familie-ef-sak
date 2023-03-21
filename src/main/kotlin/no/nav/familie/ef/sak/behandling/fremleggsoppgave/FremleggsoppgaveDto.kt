package no.nav.familie.ef.sak.behandling.fremleggsoppgave

class FremleggsoppgaveDto(
    val opprettFremleggsoppgave: Boolean
)

fun Fremleggsoppgave.tilDto(): FremleggsoppgaveDto {
    return FremleggsoppgaveDto(this.opprettFremleggsoppgave)
}