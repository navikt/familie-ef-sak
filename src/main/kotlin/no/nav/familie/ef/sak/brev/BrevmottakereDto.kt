package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson

data class BrevmottakereDto(val personer: List<BrevmottakerPerson>,
                            val organisasjoner: List<BrevmottakerOrganisasjon>)
