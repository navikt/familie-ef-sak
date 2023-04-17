# language: no
# encoding: UTF-8

Egenskap: Vedtak med midlertidig opphør

  Scenario: Revurdering legger inn midlertidig opphør, som då ikke har med inntektsperiode for opphøret

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksperiode     | Aktivitet            |
      | 1            | 05.2022         | 04.2023         | HOVEDPERIODE       | BARN_UNDER_ETT_ÅR    |
      | 2            | 07.2022         | 07.2022         | MIDLERTIDIG_OPPHØR | IKKE_AKTIVITETSPLIKT |
      | 2            | 08.2022         | 04.2023         | HOVEDPERIODE       | BARN_UNDER_ETT_ÅR    |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 05.2022         | 100     | 0                  |
      | 2            | 08.2022         | 222     | 0                  |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 05.2022         | 06.2022         | SPLITTET     | 2                     |
      | 1            | 07.2022         | 04.2023         | FJERNET      | 2                     |
      | 2            | 08.2022         | 04.2023         |              |                       |

    Så forvent følgende vedtaksperioder fra dato: 05.2022
      | Fra og med dato | Til og med dato | Vedtaksperiode | Aktivitet         |
      | 05.2022         | 06.2022         | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR |
      | 08.2022         | 04.2023         | HOVEDPERIODE   | BARN_UNDER_ETT_ÅR |

    Så forvent følgende inntektsperioder fra dato: 05.2022
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 05.2022         | 100     | 0                  |
      | 08.2022         | 222     | 0                  |