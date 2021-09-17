package no.nav.familie.ef.sak.vurdering.medlemskap

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad

class Medlemskapsgrunnlag(val søker: PdlSøker,
                          val medlemskapshistorikk: Medlemskapshistorikk,
                          val søknad: SøknadOvergangsstønad)