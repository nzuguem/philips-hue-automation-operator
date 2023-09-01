#!/bin/sh

HUE_BRIDGE_IP=$1
HUE_BRIDGE_API_TOKEN=$2

 kubectl get namespace | grep  -q "^philips-hue-operator" || kubectl create ns philips-hue-operator

 kubectl create cm  hue-api-url -n philips-hue-operator --from-literal=HUE_API_URL="http://${HUE_BRIDGE_IP}"

 kubectl create secret generic hue-api-token -n philips-hue-operator --from-literal=HUE_API_TOKEN=${HUE_BRIDGE_API_TOKEN}