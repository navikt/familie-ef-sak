UPDATE vilkarsvurdering SET resultat = 'OPPFYLT' WHERE resultat = 'JA';

UPDATE vilkarsvurdering SET resultat = 'IKKE_OPPFYLT' WHERE resultat = 'NEI';

UPDATE vilkarsvurdering
SET delvilkar = cast(replace(cast(delvilkar as text), '"resultat":"JA"','"resultat":"OPPFYLT"' ) as json);

UPDATE vilkarsvurdering
SET delvilkar = cast(replace(cast(delvilkar as text), '"resultat":"NEI"','"resultat":"IKKE_OPPFYLT"') as json);