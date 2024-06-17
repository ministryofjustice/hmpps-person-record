#### Creating the tables
Connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

Copy and paste the SQL from  [CPR-356.sql](./CPR-356.sql)

#### data import

A csv file will be supplied by Data Science, possibly by encrypted file transfer. Robin Linacre is currently responsible for transferring the file to us.

Compare the column names with the table splink_cluster as defined in [CPR-356.sql](./CPR-356.sql) and amend the csv file to match the column names. For example, `rename offender_id_display` to `prisoner_number`

Connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

Right-click on the `splink-cluster` table and select `Import Data`

Go through the wizard. Make sure you
- select CSV on `Import source`
- browse for the csv file on your local machine
- expand the Tables Mapping and check that all the fields in the csv match the columns in the `splink-cluster` table
- Enable `truncate target table` on the Data Load Settings page

The import will take several minutes