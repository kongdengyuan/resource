#!/bin/bash
## Modified by KONG DENGYUAN  
## 当你删除不需要的镜像后，如果需要重新上传同样的tag 镜像 ，也许你需要重启docker registry ，清除缓存！ 
## 需要修改 docker registry config.yml /etc/docker/registry/config.yml 重启生效

for D in /mnt/registry/docker/registry/v2/repositories/*; do

if [ -d "${D}" ]; then

        for R in $(ls -t ${D}/_manifests/tags/); do
            digest=$(curl  -sv https://registry.kkops.cc/v2/$(basename  ${D})/manifests/${R} -H 'accept: application/vnd.docker.distribution.manifest.v2+json' 2>&1 | grep Docker-Content-Digest | awk '{print $3}' )
            url="https://registry.kkops.cc/v2/$(basename  ${D})/manifests/$digest"
#           url=${url%$'\r'}
            echo $url
            curl -X DELETE  -s -H 'accept: application/vnd.docker.distribution.manifest.v2+json'  $url
        done
fi

done

if [ $? -eq 0 ];then

docker exec $(docker ps | grep registry | awk '{print $1}')  /bin/registry garbage-collect /etc/docker/registry/config.yml && echo "DELETE registry images success"

else

  exit 1

fi

