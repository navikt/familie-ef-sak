DO $$
    BEGIN
        IF EXISTS
            ( SELECT 1 from pg_roles where rolname='cloudsqliamuser')
        THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamuser;
            GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO cloudsqliamuser;
        END IF ;
    END
$$ ;
