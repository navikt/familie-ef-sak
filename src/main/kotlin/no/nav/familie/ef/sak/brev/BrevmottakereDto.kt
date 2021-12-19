package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.domain.Brevmottakere

data class BrevmottakereDto(val personer: List<Brevmottakere.BrevmottakerPerson>,
                            val organisasjoner: List<Brevmottakere.BrevmottakerOrganisasjon>)
