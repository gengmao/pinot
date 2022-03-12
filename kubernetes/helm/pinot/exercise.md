# Preinstall
## Install minikube
```
$ curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-amd64
$ sudo install minikube-darwin-amd64 /usr/local/bin/minikube
$ minikube start --memory 12000MB --cpus 6
$ minikube addons enable ingress
```
## Install helm 3
```
$ brew install helm
```
## Checkout this repo
```
$ git clone https://github.com/gengmao/pinot.git
$ cd pinot/kubernetes/helm/pinot
```

# Install cert-manager
Follow https://cert-manager.io/docs/installation/helm/
## Install via helm
```
$ helm repo add jetstack https://charts.jetstack.io
$ helm repo update
$ kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.7.1/cert-manager.crds.yaml
$ helm install \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.7.1
```
## Verify 
Follow https://cert-manager.io/next-docs/installation/verify/
```
$ cmctl check api
The cert-manager API is ready
```
## Create self-signed issuer and issue a tls cert for pinot ingress
Create issuer and certificates in [tls.yaml](./tls.yaml)
```
$ kubectl apply -f tls.yaml
namespace/pinot-quickstart created
issuer.cert-manager.io/selfsigned created
certificate.cert-manager.io/pinot-controller-cert created
certificate.cert-manager.io/pinot-broker-cert created

$ kubectl get certificate -n pinot-quickstart -o=jsonpath='{.items[*].spec}' | jq
{
  "dnsNames": [
    "broker.pinot.local"
  ],
  "duration": "240h0m0s",
  "issuerRef": {
    "name": "selfsigned"
  },
  "renewBefore": "120h0m0s",
  "secretName": "pinot-broker-tls"
}
{
  "dnsNames": [
    "controller.pinot.local"
  ],
  "duration": "240h0m0s",
  "issuerRef": {
    "name": "selfsigned"
  },
  "renewBefore": "120h0m0s",
  "secretName": "pinot-controller-tls"
}
```

## Install via helm with tls_enabled_values.yaml
### Configure
Copy [values.yaml](./values.yaml) to [tls_enabled_values.yaml](./tls_enabled_values.yaml) and configure the controller and broker ingress like below:
```
broker
  ingress:
    v1beta1:
      enabled: false
    v1:
      enabled: true
    annotations:
      kubernetes.io/ingress.class: nginx
    tls:
      - hosts:
          - broker.pinot.local
        secretName: pinot-broker-tls
    path: /
    hosts:
      - broker.pinot.local
```
Notice: some field indents were incorrect in [values.yaml](./values.yaml)

### Install
```
$ helm install -f tls_enabled_values.yaml -n pinot-quickstart pinot . 
NAME: pinot
LAST DEPLOYED: Fri Mar 11 15:25:42 2022
NAMESPACE: pinot-quickstart
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

### Check
```
$ kubectl get all,ingress -n pinot-quickstart
NAME                     READY   STATUS    RESTARTS   AGE
pod/pinot-broker-0       1/1     Running   0          79s
pod/pinot-controller-0   1/1     Running   0          79s
pod/pinot-minion-0       1/1     Running   0          79s
pod/pinot-server-0       1/1     Running   0          79s
pod/pinot-zookeeper-0    1/1     Running   0          79s

NAME                                TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                      AGE
service/pinot-broker                ClusterIP   10.106.199.237   <none>        8099/TCP                     80s
service/pinot-broker-headless       ClusterIP   None             <none>        8099/TCP                     80s
service/pinot-controller            ClusterIP   10.105.65.165    <none>        9000/TCP                     80s
service/pinot-controller-headless   ClusterIP   None             <none>        9000/TCP                     80s
service/pinot-minion                ClusterIP   10.104.210.117   <none>        9514/TCP                     80s
service/pinot-minion-headless       ClusterIP   None             <none>        9514/TCP                     80s
service/pinot-server                ClusterIP   10.98.11.54      <none>        8098/TCP,80/TCP              80s
service/pinot-server-headless       ClusterIP   None             <none>        8098/TCP,80/TCP              80s
service/pinot-zookeeper             ClusterIP   10.99.180.242    <none>        2181/TCP,2888/TCP,3888/TCP   79s
service/pinot-zookeeper-headless    ClusterIP   None             <none>        2181/TCP,2888/TCP,3888/TCP   80s

NAME                                READY   AGE
statefulset.apps/pinot-broker       1/1     79s
statefulset.apps/pinot-controller   1/1     79s
statefulset.apps/pinot-minion       1/1     79s
statefulset.apps/pinot-server       1/1     79s
statefulset.apps/pinot-zookeeper    1/1     79s

NAME                                         CLASS    HOSTS                    ADDRESS   PORTS     AGE
ingress.networking.k8s.io/pinot-broker       <none>   broker.pinot.local                 80, 443   80s
ingress.networking.k8s.io/pinot-controller   <none>   controller.pinot.local             80, 443   80s
```

### Check ingress
```% kubectl describe ingress -n pinot-quickstart
Name:             pinot-broker
Namespace:        pinot-quickstart
Address:          
Default backend:  default-http-backend:80 (<error: endpoints "default-http-backend" not found>)
TLS:
  pinot-broker-tls terminates broker.pinot.local
Rules:
  Host                Path  Backends
  ----                ----  --------
  broker.pinot.local  
                      /   pinot-broker:8099 (172.17.0.4:8099)
Annotations:          kubernetes.io/ingress.class: nginx
                      meta.helm.sh/release-name: pinot
                      meta.helm.sh/release-namespace: pinot-quickstart
Events:
  Type    Reason  Age    From                      Message
  ----    ------  ----   ----                      -------
  Normal  Sync    2m33s  nginx-ingress-controller  Scheduled for sync


Name:             pinot-controller
Namespace:        pinot-quickstart
Address:          
Default backend:  default-http-backend:80 (<error: endpoints "default-http-backend" not found>)
TLS:
  pinot-controller-tls terminates controller.pinot.local
Rules:
  Host                    Path  Backends
  ----                    ----  --------
  controller.pinot.local  
                          /   pinot-controller:9000 (172.17.0.7:9000)
Annotations:              kubernetes.io/ingress.class: nginx
                          meta.helm.sh/release-name: pinot
                          meta.helm.sh/release-namespace: pinot-quickstart
Events:
  Type    Reason  Age    From                      Message
  ----    ------  ----   ----                      -------
  Normal  Sync    2m33s  nginx-ingress-controller  Scheduled for sync
```
### Test ingress
Find the ingress-nginx-controller
```
$ kubectl get svc -n ingress-nginx               
NAME                                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
ingress-nginx-controller             NodePort    10.111.0.95     <none>        80:30886/TCP,443:31279/TCP   18h
ingress-nginx-controller-admission   ClusterIP   10.107.110.58   <none>        443/TCP                      18h
```
Port-forwarding
```
$ kubectl port-forward svc/ingress-nginx-controller -n ingress-nginx 8443:443
```
Connectivity test with curl
```
% curl -v -k -H "Host: controller.pinot.local" https://localhost:8443
*   Trying ::1:8443...
* Connected to localhost (::1) port 8443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* successfully set certificate verify locations:
*  CAfile: /etc/ssl/cert.pem
*  CApath: none
* TLSv1.2 (OUT), TLS handshake, Client hello (1):
* TLSv1.2 (IN), TLS handshake, Server hello (2):
* TLSv1.2 (IN), TLS handshake, Certificate (11):
* TLSv1.2 (IN), TLS handshake, Server key exchange (12):
* TLSv1.2 (IN), TLS handshake, Server finished (14):
* TLSv1.2 (OUT), TLS handshake, Client key exchange (16):
* TLSv1.2 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.2 (OUT), TLS handshake, Finished (20):
* TLSv1.2 (IN), TLS change cipher, Change cipher spec (1):
* TLSv1.2 (IN), TLS handshake, Finished (20):
* SSL connection using TLSv1.2 / ECDHE-RSA-AES128-GCM-SHA256
* ALPN, server accepted to use h2
* Server certificate:
*  subject: O=Acme Co; CN=Kubernetes Ingress Controller Fake Certificate
*  start date: Mar 11 08:01:20 2022 GMT
*  expire date: Mar 11 08:01:20 2023 GMT
*  issuer: O=Acme Co; CN=Kubernetes Ingress Controller Fake Certificate
*  SSL certificate verify result: unable to get local issuer certificate (20), continuing anyway.
* Using HTTP2, server supports multi-use
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
* Using Stream ID: 1 (easy handle 0x7f7f9580e800)
> GET / HTTP/2
> Host: controller.pinot.local
> user-agent: curl/7.77.0
> accept: */*
> 
* Connection state changed (MAX_CONCURRENT_STREAMS == 128)!
< HTTP/2 200 
< date: Fri, 11 Mar 2022 23:37:52 GMT
< content-type: text/html
< content-length: 673
< pinot-controller-host: pinot-controller-0.pinot-controller-headless.pinot-quickstart.svc.cluster.local
< pinot-controller-version: Unknown
< access-control-allow-origin: *
< 
* Connection #0 to host localhost left intact
<!doctype html><html lang="en"><head><meta charset="utf-8"/><title>Apache Pinot</title><link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Source+Sans+Pro:ital,wght@0,300;0,400;0,600;0,700;1,300;1,400;1,600;1,700&display=swap"/><link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/><link rel="icon" href="images/favicon.ico" type="image/x-icon"/><meta name="viewport" content="width=device-width,initial-scale=1,shrink-to-fit=no"><meta name="description" content="Pinot Controller UI"></head><body><div id="app"></div><noscript>Please enable JavaScript to use Apache Pinot UI</noscript><script src="./js/main.js"></script></body></html>%
```
To view the UI in browser, add following lines into /etc/hosts
```
# local pinot
127.0.0.1 controller.pinot.local
127.0.0.1 broker.pinot.local
```
Then open browser to the address
```
$ open https://controller.pinot.local:8443
```

# Find expring certificates within 30 days

The directory [cert-reporter](./cert-reporter/) has a [cert-reporter.py](./cert-reporter/cert-reporter.py) which finds TLS certificates in Kubernetes Secrets, parse their expiration dates and print if they are expiring within 30 days.
There is a [Dockerfile](./cert-reporter/Dockerfile) in it which 
## Build the docker image for minikube
```
$ eval $(minikube docker-env)
$ docker build -t cert-reporter cert-reporter                                    
...
Successfully tagged cert-reporter:latest
```

## RBAC and cronjob
[cert-reporter.yaml](./cert-reporter.yaml) create a ClusterRole which allows to read Kubernetes Secrets, bind it to the default service account with `pinot-quickstart` namespace, and create a Cronjob to run above docker image everyday
```
$ % kubectl apply -f cert-reporter.yaml                                                 
clusterrole.rbac.authorization.k8s.io/secret-reader created
clusterrolebinding.rbac.authorization.k8s.io/secret-reader created
cronjob.batch/cert-reporter created
```
It can be manually triggered
```
 % kubectl create job --from=cronjob/cert-reporter cert-reporter-001 
job.batch/cert-reporter-001 created
```
Example outputs
```
$ kubectl logs job.batch/cert-reporter-001
Listing secrets:
pinot-broker-tls will expire within 30 days at 2022-03-21 23:11:49
pinot-controller-tls will expire within 30 days at 2022-03-21 23:11:49
```