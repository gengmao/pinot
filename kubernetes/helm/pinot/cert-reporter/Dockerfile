FROM python:3

RUN pip3 install kubernetes cryptography

COPY cert-reporter.py .

CMD ["python", "cert-reporter.py"]

# FROM golang:1.16-alpine

# WORKDIR /app

# COPY go.mod ./
# COPY go.sum ./
# RUN go mod download

# COPY *.go ./

# RUN go build -o /cert-reporter

# EXPOSE 8080

# CMD [ "/cert-reporter" ]