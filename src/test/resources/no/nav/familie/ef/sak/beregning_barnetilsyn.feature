# language: no
# encoding: UTF-8

Egenskap: Beregning av barnetilsyn med flere perioder

  Scenario: Varierende utgifter og inntekter, satsperiode og antall barn for 2022

    Gitt utgiftsperioder
      | FraMåned | TilOgMedMåned | Beløp | AntallBarn |
      | 01.2022  | 07.2022       | 100   | 1          |
      | 08.2022  | 12.2022       | 20    | 1          |

    Og kontantstøtteperioder
      | FraMåned | TilOgMedMåned | Beløp |
      | 05.2022  | 09.2022       | 10    |

    Og tilleggsstønadsperioder
      | FraMåned | TilOgMedMåned | Beløp |
      | 07.2022  | 11.2022       | 15    |

    Når vi beregner perioder med barnetilsyn
    Så forventer vi følgende perioder
      | Beløp | FraMåned | TilOgMedMåned |
      | 64    | 01.2022  | 04.2022       |
      | 57.6  | 05.2022  | 06.2022       |
      | 42.6  | 07.2022  | 07.2022       |
      | 0     | 10.2022  | 11.2022       |
      | 0     | 08.2022  | 09.2022       |
      | 12.8  | 12.2022  | 12.2022       |





