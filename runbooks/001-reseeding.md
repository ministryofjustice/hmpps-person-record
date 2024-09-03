# 001 - Reseeding

This is the runbook to delete all current data from the database and repopulate the information from NOMIS and NDELIUS.
This includes switching off message processing and a UUID linking process.

## 1. Pause message consumption

Firstly, pause message consumption by adding the profile `seeding` to the spring configuration in helm.
This is done by adding it to the desired environment helm chart spring environment variable, which can be found at `helm_deploy/hmpps-person-record/values-<environment>.yaml`:

```
env:
    SPRING_PROFILES_ACTIVE: "<environment>, seeding"
```

Where `<environment>` is the values of: `dev`, `preprod` or `prod`.

Which the code change then needs a PR raising and releasing to the desired environment.

## 2. Delete existing data

Secondly, delete the already existing data (if any) from the database for specific source systems.
To do this run these SQL scripts to delete the desired data for the desired source system:

**NOMIS**:
```
delete from personrecordservice.person p where p.source_system = 'NOMIS'
```

**DELIUS**:
```
delete from personrecordservice.person p where p.source_system = 'DELIUS'
```


