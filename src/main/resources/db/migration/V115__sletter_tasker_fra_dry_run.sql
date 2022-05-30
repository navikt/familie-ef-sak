delete from task_logg where task_id in (select id from task where type = 'G-omregning');
delete from task where type = 'G-omregning';
