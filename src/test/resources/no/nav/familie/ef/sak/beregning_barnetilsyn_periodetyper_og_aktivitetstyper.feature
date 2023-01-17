# language: no
# encoding: UTF-8

Egenskap: Beregning av barnetilsyn med forskjellige periodetyper og aktivitetstyper

  # De to første periodene skal slås sammen siden de er identiske og etterfølgende perioder.
  # De resterende periodene skal beholdes adskilt og da skal vi stå igjen med tre utgiftsperioder til slutt.
  Scenario: Varierende aktivitetstyper

    Gitt utgiftsperioder
      | Fra måned | Til og med måned | Beløp | Antall barn | Vedtaksperiode | Aktivitet          |
      | 01.2022   | 03.2022          | 100   | 1           | ORDINÆR        | I_ARBEID           |
      | 04.2022   | 06.2022          | 100   | 1           | ORDINÆR        | I_ARBEID           |
      | 07.2022   | 08.2022          | 100   | 1           | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 09.2022   | 12.2022          | 100   | 1           | ORDINÆR        | I_ARBEID           |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder
      | Fra måned | Til og med måned | Beløp |
      | 01.2022   | 12.2022          | 64    |


  Scenario: Varierende periodetyper

    Gitt utgiftsperioder
      | Fra måned | Til og med måned | Beløp | Antall barn | Vedtaksperiode | Aktivitet |
      | 01.2022   | 03.2022          | 100   | 1           | ORDINÆR        | I_ARBEID  |
      | 04.2022   | 06.2022          | 0     | 1           | OPPHØR         |           |
      | 07.2022   | 08.2022          | 100   | 1           | ORDINÆR        | I_ARBEID  |
      | 09.2022   | 12.2022          | 0     | 1           | SANKSJON_1_MND |           |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder
      | Fra måned | Til og med måned | Beløp |
      | 01.2022   | 03.2022          | 64    |
      | 07.2022   | 08.2022          | 64    |

  Scenario: Vedtakshistorikk for Varierende aktivitetstype

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Antall barn | Utgifter | Arbeid aktivitet          | Vedtaksperiode | Aktivitet          |
      | 1            | INNVILGE        | 01.2021         | 01.2021         | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | I_ARBEID           |
      | 1            | INNVILGE        | 02.2021         | 02.2021         | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | ORDINÆR        | FORBIGÅENDE_SYKDOM |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Inntekt | Vedtaksperiode | Aktivitet |
      | 1            | 01.2021         | 01.2021         |              |                       | 0       | ORDINÆR        | I_ARBEID  |
      | 1            | 02.2021         | 02.2021         |              |                       | 0       | ORDINÆR        | FORBIGÅENDE_SYKDOM  |
