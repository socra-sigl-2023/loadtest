FROM mozilla/sbt:8u292_1.5.7

COPY ./ /code
WORKDIR /code

RUN sbt compile