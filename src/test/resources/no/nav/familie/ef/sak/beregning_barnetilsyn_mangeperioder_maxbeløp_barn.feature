# language: no
# encoding: UTF-8

# Gitt Perioder 2022:
# JAN-----------------------AUG-------------------------- DES (Utgiftsperioder )
#         MAI--------------------------SEP                    (Kontantstøtte)
#                   JUL--------------------------NOV          (Tillesstønad)
#
# JAN-----MAI-------JUL-----AUG--------SEP-------NOV----- DES (Resultat = 6)

# Formel beløp når vi ikke treffer maxgrense: ((utgifter - kontantstøtte) * 0.64 ) - reduksjonsbeløp
# Eksempel beløp Juli som har både kontantstøtte og reduksjonsbeløp: ((100 - 10) * 0.64) - 15 = *42.6*

Egenskap: Beregning av barnetilsyn med flere perioder

  Scenario: Varierende utgifter og inntekter, satsperiode og antall barn for 2022 - i denne testen er det ikke endret beløp som fører til mange perioder, men endret grunnlag

    Gitt utgiftsperioder
      | Fra måned | Til og med måned | Beløp  | Antall barn |
      | 01.2022   | 07.2022          | 100000 | 1           |
      | 08.2022   | 12.2022          | 100000 | 2           |

    Og kontantstøtteperioder
      | Fra måned | Til og med måned | Beløp |
      | 05.2022   | 09.2022          | 100   |

    Og tilleggsstønadsperioder
      | Fra måned | Til og med måned | Beløp |
      | 07.2022   | 11.2022          | 200   |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder med riktig grunnlagsdata
      | Fra måned | Til og med måned | Beløp | Har kontantstøtte | Har tilleggsstønad | Antall barn |
      | 01.2022   | 04.2022          | 4250  |                   |                    | 1           |
      | 05.2022   | 06.2022          | 4250  | x                 |                    | 1           |
      | 07.2022   | 07.2022          | 4250  | x                 | x                  | 1           |
      | 08.2022   | 09.2022          | 5545  | x                 | x                  | 2           |
      | 10.2022   | 11.2022          | 5545  |                   | x                  | 2           |
      | 12.2022   | 12.2022          | 5545  |                   |                    | 2           |





