package no.nav.familie.ef.sak.vurdering.medlemskap

import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.kontrakter.ef.søknad.Søknad

class Medlemskapsgrunnlag(val søker: PdlSøker,
                          val medlemskapshistorikk: Medlemskapshistorikk,
                          val søknad: Søknad)