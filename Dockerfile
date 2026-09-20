FROM eclipse-temurin:25-jdk

RUN --mount=type=bind,source=target,target=/build \
    mkdir /app && cd /app && jar -xf /build/*.jar

ENTRYPOINT ["java","-cp","/app/BOOT-INF/classes:/app/BOOT-INF/lib/*","br.com.erudio.StartupKt"]
