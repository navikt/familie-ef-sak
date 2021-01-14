package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate

data class Bosituasjon(@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "deler_du_bolig_")
                       val delerDuBolig: EnumTekstverdiMedSvarId,
                       @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "samboer_")
                       val samboer: PersonMinimum? = null,
                       val sammenflyttingsdato: LocalDate? = null,
                       val datoFlyttetFraHverandre: LocalDate? = null,
                       @Column("tidligere_samboer_fortsatt_registrert_pa_adresse")
                       val tidligereSamboerFortsattRegistrertPåAdresse: Dokumentasjon? = null)


