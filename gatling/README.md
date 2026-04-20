# hmpps-person-record-performance-tests
Gatling performance tests for verifying hmpps-person-record endpoints.

## Getting started.

### Adding a new test
- Add the new endpoint URI and sql in `applicaion.conf`
- Add the chain builder for the new endpoint in `ApiHelper` file
- Feed the data and add the scenario in `CorePersonRecordSimulation` file
- Inject the scenario inside **init** block in `CorePersonRecordSimulation` file


### Setting up load
- Profiles are injected from `simulation.conf` file
- Calculate number of users per second based on the required requests during a certain time
- Add that number for the new endpoint
- New profile can be created and injected during runtime


### Running tests in local
Port forward to [Access the DEV RDS Database](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html#accessing-your-rds-database)

Update GATLING_CLIENT_ID and GATLING_CLIENT_SECRET in [run_local.sh](run_local.sh)

Run [run_local.sh](run_local.sh)

### Viewing Test Results

After running the tests, the results will be available in the `build/reports/gatling` directory. Open the `index.html` file in a web browser to view the results.

For more information about the Gatling tests, see the [Gatling README](README.md).