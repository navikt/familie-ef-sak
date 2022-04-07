# language: no
# encoding: UTF-8

Egenskap: Beregning av barnetilsyn (en periode uten at vi runder av beløp)

  Scenariomal: Varierende utgifter og inntekter, satsperiode og antall barn for 2022

    Gitt følgende data
      | Periodeutgift   | Kontantstøttebeløp   | Tillegsstønadbeløp   | Antall barn   | Periodedato   |
      | <Periodeutgift> | <Kontantstøttebeløp> | <Tillegsstønadbeløp> | <Antall barn> | <Periodedato> |

    Når vi beregner barnetilsyn beløp

    Så forventer vi barnetilsyn periodebeløp
      | Beløp   |
      | <Beløp> |


    Eksempler:
      | Periodeutgift | Kontantstøttebeløp | Tillegsstønadbeløp | Antall barn | Periodedato | Beløp  |
      | 1000000       | 0                  | 0                  | 3           | 01.2022     | 6284   |
      | 1000000       | 0                  | 0                  | 4           | 01.2022     | 6284   |
      | 1000000       | 0                  | 0                  | 2           | 01.2022     | 5545   |
      | 1000000       | 0                  | 0                  | 1           | 01.2022     | 4250   |
      | 100           | 0                  | 0                  | 2           | 01.2022     | 64     |
      | 1234          | 300                | 0                  | 2           | 01.2022     | 597.76 |
      | 500.13        | 300.13             | 0                  | 3           | 01.2022     | 128    |
      | 7000          | 0                  | 70                 | 3           | 01.2022     | 4410   |
      | 7000          | 0                  | 70                 | 2           | 01.2022     | 4410   |
      | 7000          | 0                  | 70                 | 1           | 01.2022     | 4250   |
      | 3200          | 0                  | 0                  | 1           | 01.2022     | 2048   |
      | 5000          | 0                  | 0                  | 1           | 01.2022     | 3200   |
      | 7000          | 0                  | 0                  | 1           | 01.2022     | 4250   |
      | 6000          | 6000               | 0                  | 1           | 01.2022     | 0      |
      | 6000          | 1500               | 0                  | 1           | 01.2022     | 2880   |
      | 6000          | 1500               | 980                | 1           | 01.2022     | 1900   |
      | 6400          | 1500               | 0                  | 2           | 01.2022     | 3136   |
      | 9000          | 0                  | 0                  | 2           | 01.2022     | 5545   |
      | 9600          | 0                  | 0                  | 3           | 01.2022     | 6144   |
      | 12800         | 1500               | 0                  | 4           | 01.2022     | 6284   |
      | 12800         | 0                  | 0                  | 4           | 01.2022     | 6284   |
      | 12800         | 0                  | 0                  | 4           | 12.2021     | 6203   |
      | 9000          | 0                  | 0                  | 3           | 12.2021     | 5760   |
      | 12000         | 7500               | 0                  | 3           | 01.2022     | 2880   |
      | 3600          | 6000               | 0                  | 1           | 01.2022     | 0      |
      | 7000          | 4500               | 0                  | 1           | 01.2022     | 1600   |
      | 3500          | 0                  | 2940               | 1           | 01.2022     | 0      |
      | 8000          | 0                  | 0                  | 2           | 01.2022     | 5120   |
      | 11200         | 0                  | 0                  | 4           | 01.2022     | 6284   |
      | 16000         | 0                  | 0                  | 4           | 01.2022     | 6284   |






