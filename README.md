# Betting Engine Kafka

This repository targets a local Kubernetes workflow that is fast to start, test, and remove on both:
- Windows with Docker Desktop
- Linux with Docker Engine or Docker Desktop

The local Kubernetes standard is `kind`.

## Prerequisites
- Java 21
- Docker Desktop on Windows, or Docker Engine / Docker Desktop on Linux
- `kind`
- `kubectl`
- the project build wrapper once the application is implemented

Local defaults used in this README:
- cluster name: `betting-engine-local`
- namespace: `betting-engine-local`
- Kubernetes manifests: `k8s/local`

## Start the local cluster and deploy the stack

Windows PowerShell:

```powershell
kind create cluster --name betting-engine-local --wait 120s; if ($?) { kubectl create namespace betting-engine-local --dry-run=client -o yaml | kubectl apply -f - }; if ($?) { kubectl apply -n betting-engine-local -k .\k8s\local }
```

Linux:

```bash
kind create cluster --name betting-engine-local --wait 120s && kubectl create namespace betting-engine-local --dry-run=client -o yaml | kubectl apply -f - && kubectl apply -n betting-engine-local -k ./k8s/local
```

The implementation should keep this workflow one-line and fast. The local manifests should deploy:
- the betting engine service
- Kafka
- RocketMQ
- `nginx`

## Run tests

Integration tests must run against the local Kubernetes deployment, not only isolated unit or container tests. The implementation should provide a one-line wrapper command for this workflow.

Expected Windows command shape:

```powershell
.\mvnw.cmd verify -Plocal-k8s -Dk8s.cluster.name=betting-engine-local -Dk8s.namespace=betting-engine-local
```

Expected Linux command shape:

```bash
./mvnw verify -Plocal-k8s -Dk8s.cluster.name=betting-engine-local -Dk8s.namespace=betting-engine-local
```

If Gradle is chosen instead of Maven, provide an equivalent one-line wrapper command and keep this README updated.

End-to-end verification should confirm:
- the HTTP API accepts an event outcome
- the service publishes to Kafka topic `event-outcomes`
- the Kafka consumer finds matching bets from the in-memory database
- the service publishes settlement messages to RocketMQ topic `bet-settlements`
- the deployed Kubernetes resources are healthy

## Remove the local deployment or namespace

Windows PowerShell:

```powershell
kubectl delete namespace betting-engine-local --ignore-not-found=true
```

Linux:

```bash
kubectl delete namespace betting-engine-local --ignore-not-found=true
```

Use this when you want to clear the deployed resources but keep the cluster.

## Remove the local cluster completely

Windows PowerShell:

```powershell
kubectl delete namespace betting-engine-local --ignore-not-found=true; kind delete cluster --name betting-engine-local
```

Linux:

```bash
kubectl delete namespace betting-engine-local --ignore-not-found=true && kind delete cluster --name betting-engine-local
```

Use this when you want a full local reset.
