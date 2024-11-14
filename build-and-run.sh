#! /usr/bin/bash

mvn -B clean package -Dmaven.test.skip=true && mvn dependency:copy-dependencies

[ -n "${BASE_URL}" ] || BASE_URL="${1:-http://hiring.axreng.com/}"

java -DBASE_URL=${BASE_URL} -cp $({ find target/dependency/ -name '*.jar' | xargs -ri bash -c "echo $(pwd)/{}" | tr \\n : ; echo "$(pwd)/target/backend-test-1.0-SNAPSHOT.jar" ; }) com.axreng.backend.Main
