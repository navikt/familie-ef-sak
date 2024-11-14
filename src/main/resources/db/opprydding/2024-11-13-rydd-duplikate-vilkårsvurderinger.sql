/*
Skal slette duplikate vilkår som er opprettet på samme behandling.
Pga heng i systemet ble det opprettet for mange vilkår på to behandlinger.
For å kunne sette constraint i databasen for å unngå at dette skjer igjen må vi rydde opp.
Denne oppryddingen gjøres manuelt i databasen 13. november 2024.

Disse vilkårsvurderingene ble beholdt:
vilkarsvurdering_id,                 behandling_id,                       type,       barn_id
cda5d99a-5abc-45e5-a57b-f2ad23b09ae8,0b31cc8a-0a26-4d9b-a30f-58db70af175a,AKTIVITET,
94b9ba4a-b565-4908-adf7-c44ef655173c,0b31cc8a-0a26-4d9b-a30f-58db70af175a,ALENEOMSORG,6b780060-81fe-4b54-8df8-b80cc1af6f97
b9c6a9a8-719f-4fcb-9f28-6f86ed88c9cc,0b31cc8a-0a26-4d9b-a30f-58db70af175a,ALENEOMSORG,c7f8fca8-23f0-4029-a7bd-59e17b5892d8
8e68f777-23da-4e78-9da0-d7685b1f5953,0b31cc8a-0a26-4d9b-a30f-58db70af175a,ALENEOMSORG,cd40738d-d25d-453c-bc25-dd081f24ca3b
5a84a174-0ddd-40a1-ba7f-e6fd4f2fd119,0b31cc8a-0a26-4d9b-a30f-58db70af175a,ALENEOMSORG,d0852b74-a394-41d2-8830-4f47eac60f4f
dae25bcc-2847-4faf-a690-9eaef88d464c,0b31cc8a-0a26-4d9b-a30f-58db70af175a,FORUTGÅENDE_MEDLEMSKAP,
4081aefe-576d-440f-b5ce-30ea53efc9ed,0b31cc8a-0a26-4d9b-a30f-58db70af175a,LOVLIG_OPPHOLD,
b549d5e8-d69d-4ba6-af54-87e78e99bc65,0b31cc8a-0a26-4d9b-a30f-58db70af175a,MOR_ELLER_FAR,
e1438780-3035-45c5-8b9c-5e19271d5a10,0b31cc8a-0a26-4d9b-a30f-58db70af175a,NYTT_BARN_SAMME_PARTNER,
b6c289fe-a392-4709-8a14-512b83a3aee1,0b31cc8a-0a26-4d9b-a30f-58db70af175a,SAGT_OPP_ELLER_REDUSERT,
37927447-917e-4ded-9012-8ad49da7a57b,0b31cc8a-0a26-4d9b-a30f-58db70af175a,SAMLIV,
8f104530-7785-481f-97e0-9b29482c3375,0b31cc8a-0a26-4d9b-a30f-58db70af175a,SIVILSTAND,
24455c56-9d6a-4641-ad30-92307f26bc75,0b31cc8a-0a26-4d9b-a30f-58db70af175a,TIDLIGERE_VEDTAKSPERIODER,
c8a98a90-aad3-4f10-8015-f491cb6aabf4,14c94f9b-e144-4d85-8c00-1e5c466012c1,AKTIVITET,
382be10a-a4b9-44ee-9aa7-47023aff6146,14c94f9b-e144-4d85-8c00-1e5c466012c1,ALENEOMSORG,5cc1b7dd-9a38-4322-865b-df6c9bc95dcc
024c7b9c-1924-4072-89de-944977834aee,14c94f9b-e144-4d85-8c00-1e5c466012c1,ALENEOMSORG,685e022e-e43d-4993-9a8f-60aad815f3cd
148a1569-5548-4bbc-80e9-c011b8fc43c8,14c94f9b-e144-4d85-8c00-1e5c466012c1,FORUTGÅENDE_MEDLEMSKAP,
1a1ef500-b366-46ac-b1f6-27d137411408,14c94f9b-e144-4d85-8c00-1e5c466012c1,LOVLIG_OPPHOLD,
9889f11f-05db-4bf4-8679-00acf6e75871,14c94f9b-e144-4d85-8c00-1e5c466012c1,MOR_ELLER_FAR,
4b7ab782-6959-4af2-9fcc-2da86cdd1391,14c94f9b-e144-4d85-8c00-1e5c466012c1,NYTT_BARN_SAMME_PARTNER,
776f7de9-8d6c-4fea-9a26-22f4894fe02d,14c94f9b-e144-4d85-8c00-1e5c466012c1,SAGT_OPP_ELLER_REDUSERT,
bc3bbfaa-39b1-4537-be9c-d0f523ad2d70,14c94f9b-e144-4d85-8c00-1e5c466012c1,SAMLIV,
05754720-e043-4d1a-ba0e-0bc13f9c8d8d,14c94f9b-e144-4d85-8c00-1e5c466012c1,SIVILSTAND,
b0dda28a-93d4-4246-989d-85bea75e159d,14c94f9b-e144-4d85-8c00-1e5c466012c1,TIDLIGERE_VEDTAKSPERIODER,
*/

-- Script for å rydde opp i vilkår som er duplikate:
DELETE FROM vilkarsvurdering WHERE id='3b21c718-93c5-4a33-a770-5482d0eeba95' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='9d3d6644-0c23-424d-b770-72b2d61c2fa7' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='154904a0-8629-4ea1-bd99-b6d66e570298' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='bef60a59-ebff-40dd-a3af-841ad0ff910a' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='fd2ed5ef-5ae1-44d1-8e3b-289534dc4bf5' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='ec56f6fd-c5ec-4a58-b477-4f353c18b84e' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='6b780060-81fe-4b54-8df8-b80cc1af6f97';
DELETE FROM vilkarsvurdering WHERE id='c75315e7-d190-46fe-b844-1392c619d61c' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='6b780060-81fe-4b54-8df8-b80cc1af6f97';
DELETE FROM vilkarsvurdering WHERE id='53871a8d-e76d-47a3-a697-acd23bb9d60a' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='6b780060-81fe-4b54-8df8-b80cc1af6f97';
DELETE FROM vilkarsvurdering WHERE id='63442d16-9097-455d-8aa0-925bf5e1d8e3' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='6b780060-81fe-4b54-8df8-b80cc1af6f97';
DELETE FROM vilkarsvurdering WHERE id='21be857d-76fa-49b6-95dd-ba0e5f629d92' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='6b780060-81fe-4b54-8df8-b80cc1af6f97';
DELETE FROM vilkarsvurdering WHERE id='e1d987dd-922c-4509-8c5c-3ab7760f597e' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='c7f8fca8-23f0-4029-a7bd-59e17b5892d8';
DELETE FROM vilkarsvurdering WHERE id='343f1eb9-acac-41ef-9699-13df1e98296e' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='c7f8fca8-23f0-4029-a7bd-59e17b5892d8';
DELETE FROM vilkarsvurdering WHERE id='0544c455-08a2-401e-b0fb-75960ff23cb8' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='c7f8fca8-23f0-4029-a7bd-59e17b5892d8';
DELETE FROM vilkarsvurdering WHERE id='7d8c4ca7-dd26-4d78-8d1a-03ac16e33a7f' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='c7f8fca8-23f0-4029-a7bd-59e17b5892d8';
DELETE FROM vilkarsvurdering WHERE id='6b031559-893f-467a-9c1e-1ce82925672c' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='c7f8fca8-23f0-4029-a7bd-59e17b5892d8';
DELETE FROM vilkarsvurdering WHERE id='8b72954c-06f9-473e-8e64-fa35c6a44b83' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='cd40738d-d25d-453c-bc25-dd081f24ca3b';
DELETE FROM vilkarsvurdering WHERE id='5389efae-58ec-4cb5-82f3-171527e272f9' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='cd40738d-d25d-453c-bc25-dd081f24ca3b';
DELETE FROM vilkarsvurdering WHERE id='c9e24e4d-1763-4955-9ab1-ef9cab8f4c45' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='cd40738d-d25d-453c-bc25-dd081f24ca3b';
DELETE FROM vilkarsvurdering WHERE id='14673a98-1d60-4dd2-81d8-6db7ad4c00db' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='cd40738d-d25d-453c-bc25-dd081f24ca3b';
DELETE FROM vilkarsvurdering WHERE id='fbb6846f-9ebb-4fe6-bf98-8da1d7d622dd' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='cd40738d-d25d-453c-bc25-dd081f24ca3b';
DELETE FROM vilkarsvurdering WHERE id='917392a8-bba5-4271-872a-f77afb981552' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='d0852b74-a394-41d2-8830-4f47eac60f4f';
DELETE FROM vilkarsvurdering WHERE id='a66fcb87-9a5c-476d-9e14-cf3f88b096d7' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='d0852b74-a394-41d2-8830-4f47eac60f4f';
DELETE FROM vilkarsvurdering WHERE id='5deac504-8236-4f98-9d75-25aebd63b637' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='d0852b74-a394-41d2-8830-4f47eac60f4f';
DELETE FROM vilkarsvurdering WHERE id='4518e61b-b7c6-41fe-a9fb-2773ca74315e' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='d0852b74-a394-41d2-8830-4f47eac60f4f';
DELETE FROM vilkarsvurdering WHERE id='eceabaa3-ea62-4c7f-b31f-810d90c241ad' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='ALENEOMSORG' AND barn_id='d0852b74-a394-41d2-8830-4f47eac60f4f';
DELETE FROM vilkarsvurdering WHERE id='80989d57-509e-4071-9453-800d91050fdf' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='95f61dad-2755-4504-b294-0b494572d46b' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='728bd88e-fe90-4b60-aece-4ce6ad72f1a5' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='877008a3-44a7-4f21-8d69-c715ae8a6445' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='6c0738d5-a469-43be-8327-af0bf7a18513' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='ea15f3fc-cf9a-4aec-8e7a-824ad2c2d2e7' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='a6fd5680-ff73-4379-81be-8c256030fc20' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='075bd7fc-d553-4756-ae07-f3fa8888e612' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='d5470386-ddfd-4c31-9f66-398360604eb8' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='0c8ae5e8-6579-4c65-baf0-047229b382dd' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='1c7fdb1c-8f27-4d05-a3ec-9af615ed594a' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='3c8d6aa6-abc6-4d2d-b2f1-ca709514bd9b' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='53f512b5-12fe-4a6f-afc8-09e9bdf29fe8' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='3328f1ac-883d-4be4-8d49-e22cc61b8581' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='c62724be-d676-46cd-bd24-bf999e8c2ab9' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='7429ce9b-f83f-4a5c-8743-cfc35493edda' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='81ee2689-d369-4de3-a908-15ca4cf08db5' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='eeecdca7-1d69-4c6c-b383-72c8ed46d4d2' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='110239e4-3e56-4b6b-b5d1-be8184d47fb4' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='212f6a54-4775-446b-8367-ef830d710d35' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='1ed9892d-8324-4a13-b9e5-43720c7c9f9b' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='941f11c3-f217-4b96-910d-a0859685be29' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='eb53a6b6-57e7-4970-ba22-99c5afcb6a9d' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='635ea05f-c9de-44e3-a25a-44dc8aa9ee28' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='61b4afbd-bd9c-43c9-8ab9-8660605a4547' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='bfc04549-2489-4bcc-83f2-b57730bcf9c5' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='ff8a49ad-39b6-4bd7-a9e6-d83b13eae678' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='75817ab2-5cea-45f9-95f9-28edbb7f7f0f' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='c0bd05b7-af76-41d3-82c8-2fb30c4a5030' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='24c4a117-7385-4b67-b745-4610e0ee701a' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='b7f49352-86b2-49ba-be32-7215037d089b' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='349edf4f-8da9-4269-b32a-a9eebd9a9473' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='41ad4c8b-a021-4fc1-9584-33ce6724d1c9' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='bf9674af-a004-40f8-8c00-ac682aa37976' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='0e029249-ddc0-410e-af90-4fcfb7ad16e5' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='e6232ff2-1480-48d5-8b4c-a8ded58ce58a' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='TIDLIGERE_VEDTAKSPERIODER';
DELETE FROM vilkarsvurdering WHERE id='3f458bff-fc8e-46b5-b0f9-cf179511a077' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='TIDLIGERE_VEDTAKSPERIODER';
DELETE FROM vilkarsvurdering WHERE id='8853ef32-4901-4799-abae-677837f9ddcc' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='TIDLIGERE_VEDTAKSPERIODER';
DELETE FROM vilkarsvurdering WHERE id='92368e3a-40ef-4a83-9339-a02c2f813879' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='TIDLIGERE_VEDTAKSPERIODER';
DELETE FROM vilkarsvurdering WHERE id='b01e3ec8-ca2c-4ce9-9fb8-fac17c9764c3' AND behandling_id='0b31cc8a-0a26-4d9b-a30f-58db70af175a' AND type='TIDLIGERE_VEDTAKSPERIODER';
DELETE FROM vilkarsvurdering WHERE id='8214cbdc-0e9d-4b3e-9f0d-3992b3140765' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='1f8ab4f0-6441-484b-b2d8-bc794f5d4e16' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='AKTIVITET';
DELETE FROM vilkarsvurdering WHERE id='1d4deb22-c5c1-4e1d-80b9-cbdadbc078d3' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='ALENEOMSORG' AND barn_id='5cc1b7dd-9a38-4322-865b-df6c9bc95dcc';
DELETE FROM vilkarsvurdering WHERE id='791c020e-cf49-46d6-8b65-a424f79cdd5f' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='ALENEOMSORG' AND barn_id='5cc1b7dd-9a38-4322-865b-df6c9bc95dcc';
DELETE FROM vilkarsvurdering WHERE id='3493352f-56d2-4d62-964c-f973a33e701a' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='ALENEOMSORG' AND barn_id='685e022e-e43d-4993-9a8f-60aad815f3cd';
DELETE FROM vilkarsvurdering WHERE id='c7a72ef7-b238-4c6f-9723-cdbf13419659' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='ALENEOMSORG' AND barn_id='685e022e-e43d-4993-9a8f-60aad815f3cd';
DELETE FROM vilkarsvurdering WHERE id='dd6d5a27-9617-48e1-adaf-f0bf4ab6fe4f' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='28eff36a-d139-4a8e-ac87-8e475efe9128' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='FORUTGÅENDE_MEDLEMSKAP';
DELETE FROM vilkarsvurdering WHERE id='352ad424-c298-410d-8579-1bb8eb26e0a2' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='48baf3d9-23e9-47fb-8449-863002281bd2' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='LOVLIG_OPPHOLD';
DELETE FROM vilkarsvurdering WHERE id='7e39c2cc-7d9c-4a92-b507-2682949c880e' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='005f736e-4a91-44ef-876b-e20e84b8e035' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='MOR_ELLER_FAR';
DELETE FROM vilkarsvurdering WHERE id='997e2611-dd1d-45a0-ae5a-dae76bb2e576' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='48f5ce2f-dd05-4c68-89fa-41c5012a8a40' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='NYTT_BARN_SAMME_PARTNER';
DELETE FROM vilkarsvurdering WHERE id='3ae79665-886b-4c2b-9860-a5f0921be946' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='7d7420f0-c519-4b1e-a03d-801691c80731' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SAGT_OPP_ELLER_REDUSERT';
DELETE FROM vilkarsvurdering WHERE id='1060a5fa-6970-4460-bbaf-b1a49fa53689' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='e43557d6-71cf-4a9f-bbc3-f3453a78550f' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SAMLIV';
DELETE FROM vilkarsvurdering WHERE id='aa6426e2-a6f4-45b7-8219-1d6cf2d2d680' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='4f0d0587-7824-4d45-8277-b0fbda93246d' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='SIVILSTAND';
DELETE FROM vilkarsvurdering WHERE id='62a8a641-9de3-4d45-8947-3da8328ee71e' AND behandling_id='14c94f9b-e144-4d85-8c00-1e5c466012c1' AND type='TIDLIGERE_VEDTAKSPERIODER';
