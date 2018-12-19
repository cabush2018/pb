#!/bin/bash
# call: ./post.sh datacenterId:1 itemType:DATACENTER itemId:1 created:1545143491111
curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST \
--data '{\"dataCenterId\":\"${1}\", \"itemType\":\"${2}\",\"attachToServerId\":\"${3\}\",\"created\":\"${5}\"}' http://localhost:8080/fullfillment