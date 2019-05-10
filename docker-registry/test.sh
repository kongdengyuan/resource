for D in /mnt/registry/docker/registry/v2/repositories/*; do

if [ -d "${D}" ]; then

        for R in $(ls -t ${D}/_manifests/tags/); do
            digest=$(curl  -sv https://registry.kkops.cc/v2/$(basename  ${D})/manifests/${R} -H 'accept: application/vnd.docker.distribution.manifest.v2+json' 2>&1 | grep Docker-Content-Digest | awk '{print $3}' )
            url="https://registry.kkops.cc/v2/$(basename  ${D})/manifests/$digest"
#           url=${url%$'\r'}
            echo $url
done
fi
done


