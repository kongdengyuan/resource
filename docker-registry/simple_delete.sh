#!/bin/bash
#curl https://raw.githubusercontent.com/burnettk/delete-docker-registry-image/master/delete_docker_registry_image.py | sudo tee /usr/local/bin/delete_docker_registry_image >/dev/null

#chmod a+x /usr/local/bin/delete_docker_registry_image 

## refrence  https://www.jianshu.com/p/cb3def675093

## Get images list
IMAGES=`curl -s  https://registry.kkops.cc/v2/_catalog  | sed -e 's#^.*:\[\(.*\)\]}.*$#\1#' -e 's#"##g' -e 's#,# #g'`

for i in $IMAGES ; do 

delete_docker_registry_image --image $i 

done 

echo "Delete images success" 
