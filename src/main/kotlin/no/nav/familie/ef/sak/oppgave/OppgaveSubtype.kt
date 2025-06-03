package no.nav.familie.ef.sak.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

enum class OppgaveSubtype {
    INFORMERE_OM_SØKT_OVERGANGSSTØNAD,
    INNSTILLING_VEDRØRENDE_UTDANNING,
}

enum class OppgaveTypeForBeslutter (val besluttOppgaveType: Oppgavetype) {
    Fremlegg(Oppgavetype.Fremlegg),
    VurderHenvendelse(Oppgavetype.VurderHenvendelse),
    VurderKonsekvensForYtelse(Oppgavetype.VurderKonsekvensForYtelse),
}