# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Enkel innvilget behandling av typen barnetilsyn

  Scenario: Innvilget barnetilsyn skal gi informasjon om antall barn, utgifter, kontantstøtte

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Vedtaksperiode | Aktivitet |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | I_ARBEID  |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp | Arbeid aktivitet          | Aktivitet |
      | 1            |              |                       | 10            | 15             | 1           | 200      | 107   | ETABLERER_EGEN_VIRKSOMHET | I_ARBEID  |


  Scenario: Innvilget barnetilsyn med 0 barn gir 0 i stønadsbeløp

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Vedtaksperiode | Aktivitet |
      | 1            | INNVILGE        | 0           | 200      | ORDINÆR        | I_ARBEID  |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |

    Når beregner ytelse kaster feil

    Så forvent følgende feil: Må ha med minst et barn på en periode som ikke er et midlertidig opphør

  Scenario: Vedtakshistorikk for Varierende aktivitetstype

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Barn | Utgifter | Arbeid aktivitet          | Vedtaksperiode | Aktivitet          |
      | 1            | INNVILGE        | 01.2021         | 01.2021         | A    | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | I_ARBEID           |
      | 1            | INNVILGE        | 02.2021         | 02.2021         | A    | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 1            | INNVILGE        | 03.2021         | 03.2021         | B    | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 1            | INNVILGE        | 04.2021         | 04.2021         | B    | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | FORBIGÅENDE_SYKDOM |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 128   | 1               |
      | 02.2021         | 02.2021         | 128   | 1               |
      | 03.2021         | 04.2021         | 128   | 1               |

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Inntekt | Vedtaksperiode | Aktivitet          |
      | 1            | 01.2021         | 01.2021         |              |                       | 0       | ORDINÆR        | I_ARBEID           |
      | 1            | 02.2021         | 02.2021         |              |                       | 0       | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 1            | 03.2021         | 04.2021         |              |                       | 0       | ORDINÆR        | FORBIGÅENDE_SYKDOM |
