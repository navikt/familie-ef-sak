package no.nav.familie.ef.sak.integration.dto.personopplysning.status

import no.nav.familie.ef.sak.integration.dto.personopplysning.Periode

data class PersonstatusPeriode(val periode: Periode,
                               val personstatus: PersonstatusType)

