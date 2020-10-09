package no.nav.familie.ef.sak.repository.domain.søknad

data class SøknadBarnetilsyn(val personalia: Personalia,
                             val innsendingsdetaljer: Innsendingsdetaljer,
                             val sivilstandsdetaljer: Sivilstandsdetaljer,
                             val medlemskapsdetaljer: Medlemskapsdetaljer,
                             val bosituasjon: Bosituasjon,
                             val sivilstandsplaner: Sivilstandsplaner? = null,
                             val barn: List<Barn>,
                             val aktivitet: Aktivitet,
                             val stønadsstart: Stønadsstart,
                             val dokumentasjon: BarnetilsynDokumentasjon)

data class BarnetilsynDokumentasjon(val barnepassordningFaktura: Dokumentasjon? = null,
                                    val avtaleBarnepasser: Dokumentasjon? = null,
                                    val arbeidstid: Dokumentasjon? = null,
                                    val roterendeArbeidstid: Dokumentasjon? = null,
                                    val spesielleBehov: Dokumentasjon? = null)
