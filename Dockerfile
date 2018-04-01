# Using an Alpine Linux based JDK image
FROM anapsix/alpine-java:8u162b12_jdk

COPY target/pack /srv/mirkoswarm

# Using a non-privileged user:
USER nobody
WORKDIR /srv/mirkoswarm

EXPOSE 2137

ENTRYPOINT ["sh", "./bin/mirko-swarm"]