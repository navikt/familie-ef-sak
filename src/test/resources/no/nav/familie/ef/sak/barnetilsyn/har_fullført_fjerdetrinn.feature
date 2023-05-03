# language: no
# encoding: UTF-8

Egenskap: Har fullført fjerdetrinn

  Scenariomal: Har fullført fjerdetrinn

    Gitt følgende fødselsdato "<Fødselsdato>" for barn

    Når sjekk på om barn har fullført fjerde skoletrinn utføres den "<Dato sjekk utføres>"

    Så forvent resultat "<Resultat>"

    Eksempler:
      | Fødselsdato | Dato sjekk utføres | Resultat |
      | 01.01.2012  | 01.07.2022         | Ja       |
      | 01.01.2013  | 01.07.2022         | Nei      |
      | 01.01.2012  | 01.06.2022         | Ja       |
      | 01.01.2012  | 01.05.2022         | Ja       |
      | 01.01.2012  | 01.04.2022         | Nei      |
      | 01.01.2012  | 01.11.2021         | Nei      |
      | 14.04.2013  | 01.04.2023         | Nei      |
      | 14.04.2013  | 01.05.2023         | Ja       |