# Requirements

- [x] threaded crawler
- [x] java spark
- [x] no 3rd party libs/frameworks
- [x] concurrent api


- POST: starts keyword search, eg:
```
Requisição:
POST /crawl HTTP/1.1
Host: localhost:4567
Content-Type: application/json
Body: {"keyword": "security"}
Answer: 200 OK
Content-Type: application/json
Body: {"id": "30vbllyb"}
```

- GET: queries the result (which will change until all the keywords are found or the limits have been reached) 
```
Requisição:
GET /crawl/30vbllyb HTTP/1.1
Host: localhost:4567
```

# Implementation

- the Limits interface has bounds for requirements, concurrency and sizing (eg. max keywords)
  These may be adjusted according to the available hardware. There is no "self-balancing". Some of the most relevant are:
  - KEYWORD_ACTIVE_MAX (100) the maximum number of "active" keyword searches in progress
  - PAGE_VISITS_PER_KEYWORD_MAX (1000) the maximum number of links followed in search for a keyword (this has been tested with a 5000 value)
  - KEYWORD_WORKER_CONCURRENCY_MAX (50) the number of simultaneous searches in progress for a keyword
- an HTML library such as JSoup should be used instead of "pseudo-parsing" HTML links (but this is prevented by the requirements)
- it seems the API uses a "Body" Header (in JSON payload) for obtaining the keyword from POST requests.
  Why not use just a "Keyword" Header with a text value? The same applies to the response: why not just use an "id" (or "keyword_id") Header?
- concurrency is established via a page visit queue with multiple simultaneous keyword searches.
  There is a limit to active keyword searches. The approach chosen is to fail-fast (keyword searches are not buffered).
- link fragments and query strings are preserved in the url results (since they could be used as parameters in dynamic pages)
- there is a cache for searched keywords. This cache is configured (per search) to expire after 1 day, after that the keyword is refreshed.
  The cache makes requests idempotent while a keyword is not expired.
- api requests are rate-limited by API_REQUEST_INTERVAL_MIN_NS (200 requests/sec) per endpoint: after that a status 429 or 420 is returned 

# Tests

- most tests require internet access
- the tests may take a few minutes to complete, what is usually regarded as "very long". This time could be shortened by changing test parameters
- the log level may be adjusted by configuring the simplelogger.properties files to the required level (tests and release)
- the log level has been set to "off" to cope with the issues described below

# Issues

- the command line for running the given Dockerfile "does not work on my machine" (Fedora 39/OpenJDK 17/Podman). The command that works is:
```
  docker run -e 'BASE_URL=http://www.google.com/' -p 4567:4567 --rm axreng/backend -DBASE_URL=http://www.google.com/
```
  (the BASE_URL environment has to be appended to the original command, maybe `ENV BASE_URL ${BASE_URL}` is missing in the Dockerfile or this is a Podman issue)
- most of the time the above command fails with the following message, see [SUREFIRE-1614](https://issues.apache.org/jira/browse/SUREFIRE-1614) 
  (the doc reports the issue as fixed in 2.22.2 but it is not):
```
[WARNING] Corrupted STDOUT by directly writing to native stream in forked JVM 1. See FAQ web page and the dump file /usr/src/axreng/target/surefire-reports/2024-03-24T13-07-39_355-jvmRun1.dumpstream
```
  IF THIS HAPPENS THE SERVER THAT IS RUNNING IS THE UNIT TEST SERVER NOT THE VM SERVER AND BASE_URL IS THE ONE FROM THE TEST

# Scripts

A few scripts have been implemented to circumvent the issues described.

- test.sh for testing
- build-and-run.sh WORKS as expected OUTSIDE a container
- build-and-run-dockerfile.sh the commands from the spec.
- build-and-run-dockerfile-java.sh WORKS as expected INSIDE a container 
  (image axreng/backend-java, is half the size of axreng/backend and functionally equivalent with the same classes and libraries)
