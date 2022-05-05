# language: no
# encoding: UTF-8

# Gitt Perioder 2021:
# JAN --------------------- JULI --------------------------- DES (Utgiftsperioder = 2)

# Gitt Perioder 2022:
# JAN-----------------------AUG-------------------------- DES (Utgiftsperioder )
#         MAI--------------------------SEP                    (Kontantstøtte)
#                   JUL--------------------------NOV          (Tillesstønad)
#
# JAN-----MAI-------JUL-----AUG--------SEP-------NOV----- DES (Resultat = 6)

# Formel beløp når vi treffer maxgrense er gitt antall barn og max-sats for perioden
# Eksempel overgang fra 1 til 2 barn i 2021 4195->5474
# Eksempel på satsendring fra 2021 til 2022:  5474 -> 5545 (med samme antall barn)

# Til diskusjon: hull i perioder 07-09

# Til diskusjon: fornuftig/nødvendig?
# Eksempel på ny periode selv om antall barn, sats og beløp er uendret:
# 04.2022 -> 05.2022 (grunnlagsdata endret - kontantstøtte)


Egenskap: Beregning av beløp og perioder når utgifter-reduksjon er større enn maxbeløp

  Scenario: Tre utgiftsperioder (beveger seg fra 1-3 barn) og strekker seg over to år (to satsperioder).

    Gitt utgiftsperioder
      | Fra måned | Til og med måned | Beløp  | Antall barn |
      | 01.2021   | 07.2021          | 39000 | 1           |
      | 09.2021   | 07.2022          | 39000 | 2           |
      | 08.2022   | 12.2022          | 39000 | 3           |

    Og kontantstøtteperioder
      | Fra måned | Til og med måned | Beløp |
      | 05.2022   | 09.2022          | 100   |

    Og tilleggsstønadsperioder
      | Fra måned | Til og med måned | Beløp |
      | 07.2022   | 11.2022          | 200   |

    Når vi beregner perioder med barnetilsyn

    Så forventer vi følgende perioder med riktig grunnlagsdata
      | Fra måned | Til og med måned | Beløp | Har kontantstøtte | Har tilleggsstønad | Antall barn | Hvorfor ny periode her |
      | 01.2021   | 07.2021          | 4195  |                   |                    | 1           | Start                  |
      | 09.2021   | 12.2021          | 5474  |                   |                    | 2           | Nytt barn              |
      | 01.2022   | 04.2022          | 5545  |                   |                    | 2           | Ny satsperiode         |
      | 05.2022   | 06.2022          | 5545  | Ja                |                    | 2           | Får kontantstøtte      |
      | 07.2022   | 07.2022          | 5345  | Ja                | Ja                 | 2           | Får tillegsstønad      |
      | 08.2022   | 09.2022          | 6084  | Ja                | Ja                 | 3           | Nytt barn              |
      | 10.2022   | 11.2022          | 6084  |                   | Ja                 | 3           | Mister kontantstøtte   |
      | 12.2022   | 12.2022          | 6284  |                   |                    | 3           | Mister tilleggsstønad  |





