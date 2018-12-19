#!/bin/bash

curl --header "Content-Type: application/json" --noproxy localhost --silent --request POST \
--data '[{\"dataCenterId\":\"${1}\", \"itemType\":\"${2}\", \"attachToServerId\":\"${3}\", \"action\":\"${4}\"}]' http://localhost:8080/requests