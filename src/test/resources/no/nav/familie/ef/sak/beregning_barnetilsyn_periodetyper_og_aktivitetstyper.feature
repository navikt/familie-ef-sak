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
      | 07.2022   | 08.2022          | 100   | 1           | ORDINÆR        | forbigående_sykdom |
      | 09.2022   | 12.2022          | 100   | 1           | ORDINÆR        | I_ARBEID           |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder
      | Fra måned | Til og med måned | Beløp |
      | 01.2022   | 06.2022          | 64    |
      | 07.2022   | 08.2022          | 64    |
      | 09.2022   | 12.2022          | 64    |


  Scenario: Varierende periodetyper

    Gitt utgiftsperioder
      | Fra måned | Til og med måned | Beløp | Antall barn | Vedtaksperiode | Aktivitet |
      | 01.2022   | 03.2022          | 100   | 1           | ORDINÆR        | I_ARBEID  |
      | 04.2022   | 06.2022          | 0     | 1           | opphør         |           |
      | 07.2022   | 08.2022          | 100   | 1           | ORDINÆR        | I_ARBEID  |
      | 09.2022   | 12.2022          | 0     | 1           | sanksjon_1_mnd |           |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder
      | Fra måned | Til og med måned | Beløp |
      | 01.2022   | 03.2022          | 64    |
      | 04.2022   | 06.2022          | 0     |
      | 07.2022   | 08.2022          | 64    |
      | 09.2022   | 12.2022          | 0     |