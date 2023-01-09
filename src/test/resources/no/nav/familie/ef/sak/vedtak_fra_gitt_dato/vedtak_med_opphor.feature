# language: no
# encoding: UTF-8

Egenskap: hentVedtakForOvergangsstønadFraDato

  Scenario: Opphørsperioder skal ikke være med i vedtaksperioder

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Opphørsdato | Vedtaksresultat | Aktivitet         | Vedtaksperiode |
      | 1            | 01.2021         | 03.2021         |             | INNVILGE        | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |
      | 2            |                 |                 | 03.2021     | OPPHØRT         |                   |                |

    Når beregner ytelse

    Så forvent følgende vedtaksperioder fra dato: 11.2020
      | Fra og med dato | Til og med dato | Aktivitet         | Vedtaksperiode |
      | 01.2021         | 02.2021         | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |