# 005 - Run a batch job

This runbook outlines the steps to run a batch job in kubernetes.

## Identify the batch job
Run the following command to list the batch jobs in the namespace:
```shell
kubectl get cronjobs -n <namespace>
```
Grab the name of the batch job from the list, for example `hmpps-person-record-recluster-needs-attention`

## Create the batch job instance
Run the following command to create a batch job instance from the cronjob:
```shell
kubectl create job -n <namespace> --from=cronjob/<batch-job-name> <batch-job-name>-<unique-name>
```
This will create a batch job instance with the name `<batch-job-name>-<unique-name>`, and it will run immediately. You can check the status of the job with:
```shell
kubectl logs job.batch/<batch-job-name>-<unique-name>  -n <namespace> -f
```

## Overriding environment variables
If you need to override environment variables for the batch job, to change the starting page for example, you first need to create a template from a dryrun:
```shell
kubectl create job -n <namespace> --from=cronjob/<batch-job-name> <batch-job-name>-<unique-name> --dry-run=client -o yaml > job.yaml
```
This will create a local `job.yaml` file that you can edit to change various parts of the job. Once you have made your changes, you can create the job with:
```shell
kubectl create -f job.yaml
```