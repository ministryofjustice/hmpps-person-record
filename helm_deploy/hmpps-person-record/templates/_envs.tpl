    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "{{ .Values.spring.profile }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: FEATURE_FLAGS_ENABLE_CACHEABLE_CASE_LIST
    value: "{{ .Values.env.FEATURE_FLAGS_ENABLE_CACHEABLE_CASE_LIST }}"

  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: hmpps-person-record-rds-instance-output
        key: database_username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: hmpps-person-record-rds-instance-output
        key: database_password

  - name: DATABASE_NAME
    valueFrom:
      secretKeyRef:
        name: hmpps-person-record-rds-instance-output
        key: database_name

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: hmpps-person-record-rds-instance-output
        key: rds_instance_endpoint

  {{- with (index .Values.ingress.hosts 0)}}
  - name: INGRESS_URL
    value: {{ .host }}
  {{- end }}
{{- end -}}
