FROM eclipse-temurin:25-jdk

RUN --mount=type=bind,source=target,target=/build \
    mkdir -p /app/config && cd /app && jar -xf /build/*.jar \
    && echo "net.sf.jasperreports.awt.ignore.missing.font=true" > /app/config/jasperreports.properties

ENTRYPOINT ["java","-cp","/app/config:/app/BOOT-INF/classes:/app/BOOT-INF/lib/*","br.com.erudio.StartupKt"]
