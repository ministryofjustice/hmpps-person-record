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
If you need to override environment variables, to change the starting page for example, first generate a job template using a dry run:
```shell
kubectl create job -n <namespace> --from=cronjob/<batch-job-name> <batch-job-name>-<unique-name> --dry-run=client -o yaml > job.yaml
```
This will create a local `job.yaml` file that you can edit to change various parts of the job. Once you have made your changes, you can create the job with:
```shell
kubectl create -f job.yaml
```

## Copying files into the pod
If you need to copy data into the pod, for example a JSON file, you can use the `kubectl cp` command. It is recommended that you copy the file into the `/tmp` directory as that has the necessary permissions.
```shell
kubectl cp ./<local-file-name> <namespace>/<batch-job-pod-name>:/tmp/<file-name>
```