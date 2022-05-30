ARG APP_BUILDER=openjdk:11
ARG APP_RUNNER=$APP_BUILDER

FROM $APP_BUILDER as builder
ARG SERVICE

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update
RUN apt-get install sbt

WORKDIR /build
COPY ./$SERVICE ./$SERVICE
COPY ./common ./common
RUN cd $SERVICE && sbt stage


FROM $APP_RUNNER
ARG SERVICE
WORKDIR /app
COPY --from=builder /build/$SERVICE/target/universal/stage/lib lib
COPY --from=builder /build/$SERVICE/target/universal/stage/bin bin
ENV SERVICE_EXE=$SERVICE
ENTRYPOINT sleep 90 && bin/$SERVICE_EXE