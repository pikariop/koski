revoke insert, update, delete, truncate on henkilo from oph;
revoke insert, update, delete, truncate on mydata_jako from oph;
revoke insert, update, delete, truncate on opiskeluoikeus from oph;
revoke insert, update, delete, truncate on opiskeluoikeus_fixture from oph;
revoke insert, update, delete, truncate on opiskeluoikeushistoria from oph;
revoke insert, update, delete, truncate on opiskeluoikeushistoria_fixture from oph;
revoke insert, update, delete, truncate on oppilaitos_ip_osoite from oph;
revoke insert, update, delete, truncate on preferences from oph;
revoke insert, update, delete, truncate on schema_version from oph;
revoke insert, update, delete, truncate on suoritusjako from oph;
revoke insert, update, delete, truncate on suoritusjako_v2 from oph;
revoke insert, update, delete, truncate on poistettu_opiskeluoikeus from oph;
ALTER ROLE oph NOSUPERUSER CREATEDB CREATEROLE INHERIT LOGIN;
