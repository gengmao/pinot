from kubernetes import client, config
from base64 import b64decode
from cryptography import x509
from cryptography.hazmat.backends import default_backend
import datetime

REPORT_THRESHOLD_DAYS=30

def main():
    config.load_incluster_config()

    v1 = client.CoreV1Api()
    print("Listing secrets:")
    ret = v1.list_namespaced_secret("pinot-quickstart")
    for i in ret.items:
        name = i.metadata.name
        if 'tls.crt' in i.data:
            pem_data = b64decode(i.data["tls.crt"]) 
            cert = x509.load_pem_x509_certificate(pem_data, default_backend())
            expiring_days = cert.not_valid_after.date() - datetime.date.today()
            if expiring_days < datetime.timedelta(days=REPORT_THRESHOLD_DAYS):
                print("%s will expire within %d days at %s" % (name, REPORT_THRESHOLD_DAYS, cert.not_valid_after))

if __name__ == '__main__':
    main()
