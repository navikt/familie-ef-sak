# language: no
# encoding: UTF-8

Egenskap: hentVedtakForOvergangsstønadFraDato

  Scenario: Behandling med hull i historikk legger til midlertidelig opphør
    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Aktivitet          | Vedtaksperiode |
      | 1            | 02.2021         | 03.2021         | INNVILGE        | BARN_UNDER_ETT_ÅR  | HOVEDPERIODE   |
      | 2            | 06.2021         | 07.2021         | INNVILGE        | FORSØRGER_I_ARBEID | UTVIDELSE      |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 02.2021         | 10      | 20                 |
      | 2            | 06.2021         | 20      | 10                 |

    Når beregner ytelse

    # henter perioder før tidligere dato
    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Aktivitet            | Vedtaksperiode     |
      | 02.2021         | 03.2021         | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE       |
      | 04.2021         | 05.2021         | IKKE_AKTIVITETSPLIKT | HOVEDPERIODE |
      | 06.2021         | 07.2021         | FORSØRGER_I_ARBEID   | UTVIDELSE          |

    # henter perioder fra første fom-dato
    Så forvent følgende vedtaksperioder fra dato: 02.2021
      | Fra og med dato | Til og med dato | Aktivitet            | Vedtaksperiode     |
      | 02.2021         | 03.2021         | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE       |
      | 04.2021         | 05.2021         | IKKE_AKTIVITETSPLIKT | MIDLERTIDIG_OPPHØR |
      | 06.2021         | 07.2021         | FORSØRGER_I_ARBEID   | UTVIDELSE          |

    # henter perioder fra hull
    Så forvent følgende vedtaksperioder fra dato: 04.2021
      | Fra og med dato | Til og med dato | Aktivitet          | Vedtaksperiode |
      | 06.2021         | 07.2021         | FORSØRGER_I_ARBEID | UTVIDELSE      |

    # henter perioder etter tidligere perioder
    Så forvent følgende vedtaksperioder fra dato: 08.2021
      | Fra og med dato | Til og med dato | Aktivitet          | Vedtaksperiode |
