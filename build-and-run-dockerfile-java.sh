#! /usr/bin/bash

[ -n "${BASE_URL}" ] || BASE_URL="${1:-http://hiring.axreng.com/}"

bash test.sh || exit 1

mvn package -Dmaven.test.skip=true && mvn dependency:copy-dependencies

docker build -f Dockerfile-java -t axreng/backend-java

docker run -e "BASE_URL=${BASE_URL}" -p 4567:4567 --rm axreng/backend-java

