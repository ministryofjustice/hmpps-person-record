# Importing term frequency data
A csv file will be supplied by Data Science, possibly by encrypted file transfer. Robin Linacre is currently responsible for transferring the file to us.
Future: term frequency tables will be managed by the splink cluster

## Creating the schema and table
Connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

If the `personmatchscore` schema, does not exist:
Copy and paste the SQL from  [CPR-356-create-personmatchscore-schema](./CPR-356-create-personmatchscore-schema.sql) and execute it

If the `personmatchscore.last_name_frequency`, `personmatchscore.first_name_frequency` and `personmatchscore.date_of_birth_frequency` table does not exist:
Copy and paste the SQL from  [CPR-238.sql](./CPR-238.sql) and execute it

#### Data import

Compare the column names within term frequency tables as defined in [CPR-238.sql](./CPR-238.sql) and amend the csv file to match the column names. For example, `rename forename` to `first_name`

Connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

Right-click on the frequency tables e.g. `first_name_frequency` table and select `Import Data`

Go through the wizard. Make sure you
- select CSV on `Import source`
- browse for the csv file on your local machine
- expand the Tables Mapping and check that all the fields in the csv match the columns in the `first_name_frequency` table
- Enable `truncate target table` on the Data Load Settings page

The import will take a few seconds
