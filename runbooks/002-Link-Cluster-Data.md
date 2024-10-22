# 002 - Link Cluster Data

This is a runbook to link an existing cluster data to seeded data.

A csv file will be supplied by Data Science, possibly by encrypted file transfer. Robin Linacre is currently responsible for transferring the file to us.

## Prerequisites
* Must have a active connection to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

## 1. Creating the schema and table

If the `personmatchscore.splink_cluster` table does not exist:
Copy and paste the SQL from  [CPR-356-create-splink-cluster.sql](../scripts/db/CPR-356-create-splink-cluster.sql) and execute it

## 2. Data import

Compare the column names with the table splink_cluster as defined in [CPR-356-create-splink-cluster.sql](../scripts/db/CPR-356-create-splink-cluster.sql) and amend the csv file to match the column names. For example, `rename offender_id_display` to `prison_number`

Right-click on the `splink-cluster` table and select `Import Data`

Go through the wizard. Make sure you
- select CSV on `Import source`
- browse for the csv file on your local machine
- expand the Tables Mapping and check that all the fields in the csv match the columns in the `splink-cluster` table
- Enable `truncate target table` on the Data Load Settings page

The import will take several minutes


## 3. Delete the existing links

> WARNING:
> This assumes that there are no useful UUIDs links already present within the database. Will delete LIBRA + COMMON_PLATFORM UUIDs

Copy and paste the SQL from  [CPR-356-deleting-existing-links.sql](../scripts/db/CPR-356-deleting-existing-links.sql) and execute it

## 4. Populate the links

Copy and paste the SQL from  [CPR-356-generate-person-ids.sql](../scripts/db/CPR-356-generate-person-ids.sql) and execute it

## 5. Reporting on cluster data (Optional)

Copy and paste the SQL from  [CPR-414-uuid-composition-report.sql](../scripts/db/CPR-414-uuid-composition-report.sql) and execute it