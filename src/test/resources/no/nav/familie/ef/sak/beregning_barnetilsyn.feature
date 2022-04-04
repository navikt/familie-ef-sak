# language: no
# encoding: UTF-8

Egenskap: Beregning av barnetilsyn (en periode)

  Scenario: Varierende utgifter og inntekter, satsperiode og antall barn for 2022

    Gitt følgende data
      | Rad | Periodeutgift | KontrantstøtteBeløp | TillegsønadBeløp | AntallBarn | PeriodeDato | Testkommentar   |
      | 1   | 1000000       | 0                   | 0                | 3          | 01.2022     | Max sum 3 barn  |
      | 2   | 1000000       | 0                   | 0                | 4          | 01.2022     | Antall barn > 3 |
      | 3   | 1000000       | 0                   | 0                | 2          | 01.2022     | Max sum 2 barn  |
      | 4   | 1000000       | 0                   | 0                | 1          | 01.2022     | Max sum 1 barn  |
      | 5   | 100           | 0                   | 0                | 2          | 01.2022     |                 |
      | 6   | 1234          | 300                 | 0                | 2          | 01.2022     |                 |
      | 7   | 500.13        | 300.13              | 0                | 3          | 01.2022     |                 |
      | 8   | 7000          | 0                   | 70               | 3          | 01.2022     |                 |
      | 9   | 7000          | 0                   | 70               | 2          | 01.2022     |                 |
      | 10  | 7000          | 0                   | 70               | 1          | 01.2022     |                 |

    Når vi beregner barnetilsyn beløp

    Så forventer vi barnetilsyn periodebeløp
      | Rad | Beløp  |
      | 1   | 6284   |
      | 2   | 6284   |
      | 3   | 5545   |
      | 4   | 4250   |
      | 5   | 64     |
      | 6   | 597.76 |
      | 7   | 128    |
      | 8   | 4410   |
      | 9   | 4410   |
      | 10  | 4250   |


