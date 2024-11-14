#! /usr/bin/bash

docker build . -t axreng/backend

docker run -e 'BASE_URL=http://hiring.axreng.com/' -p 4567:4567 --rm axreng/backend -DBASE_URL=http://hiring.axreng.com/