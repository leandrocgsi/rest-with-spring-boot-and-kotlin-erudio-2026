# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4.1.1 / Kotlin 2.3 (JDK 25) REST API (Maven, base package `br.com.erudio`) from the Erudio course "Formação Spring Boot com Kotlin: REST APIs Profissionais do Zero ao Deploy na AWS com Docker e Kubernetes". It is a 1:1 port of the Java project `rest-with-spring-boot-and-java-erudio-2026`: same endpoints, status codes, payloads (field order, date formats, HAL links), Flyway migrations (`V1..V18`, byte-identical), Jasper templates, database (`rest_with_spring_boot_erudio`) and Postman collection (`Collections/`). The app manages People and Books, with JWT auth, file upload/download, CSV/XLSX/PDF import/export and email sending.

When changing behavior, keep it identical to the Java project unless the change is asked for explicitly. Several settings below exist only for that reason; do not "clean them up".

## Commands

There is no Maven wrapper; use the system `mvn`, with `JAVA_HOME` pointing at a JDK 25. The first build downloads the Kotlin compiler.

```bash
mvn spring-boot:run                    # run the app (needs MySQL, see below)
mvn clean package -DskipTests          # build the jar
mvn test                               # everything (needs Docker)
mvn test -Dtest=PersonServicesTest     # one test class
mvn flyway:migrate                     # apply migrations manually (pom hardcodes localhost DB + root/admin123)
docker compose up -d --build           # app on :8080 + MySQL 9 on :3308 (run "mvn clean package" first, the image copies target/*.jar)
```

Tests split into two kinds:
- `unittests/**` use Mockito (with `mockito-kotlin` for matchers, because plain `any()` returns null and Kotlin rejects it for non-null parameters) and need nothing external.
- `integrationtests/**` and `repository/PersonRepositoryTest` extend `AbstractIntegrationTest`, which starts a **Testcontainers `mysql:9.1.0`** container plus **GreenMail** (in-process SMTP, account `sender@erudio.test` / `secret`), so Docker must be running. The app then boots on port **8888** (`src/test/resources/application.yml`, `TestConfigs.SERVER_PORT`) and RestAssured calls it.

Things worth knowing about the tests:
- The integration tests are order-dependent (`@TestMethodOrder(OrderAnnotation)`, `@TestInstance(PER_CLASS)`, state kept in `lateinit` fields filled by the `@Order(0)` `signin` test). Run the whole class, not a single method. New integration tests extend `AuthenticatedIntegrationTest`, which signs in once and hands out `authenticated()` / `anonymous()` request specs.
- `AbstractIntegrationTest` uses the deprecated `org.testcontainers.containers.MySQLContainer` on purpose. The new `org.testcontainers.mysql.MySQLContainer` configures the server differently and changes which of the equally titled books MySQL returns first for `ORDER BY title`, which breaks `BookController*Test.findAllTest` (the seed has seven "The Art of Agile Development" rows).
- Import test annotations explicitly and `Assertions.*` with a star: `import org.junit.jupiter.api.*` next to `Assertions.*` makes `assertNotNull` ambiguous with JUnit 6's Kotlin assertions.
- The test `file.upload-dir` is `target/test-uploads`, not the drive root used by the main config.
- The PDF templates download images from `raw.githubusercontent.com` while the report is generated, so the PDF tests are skipped (via `NetworkAssumptions`) when that host is unreachable.
- `src/test/resources/jasperreports.properties` sets `net.sf.jasperreports.awt.ignore.missing.font=true`. `people.jrxml` asks for the `Arial` font, which Linux machines (including GitHub Actions `ubuntu-latest`) do not have, and without the flag every PDF test fails with `JRFontNotFoundException`. JasperReports only reads this flag from a `jasperreports.properties` on the classpath, not from `-D`. Windows/macOS developers never see the problem, so check the PDF tests in a Linux container (`maven:3.9-eclipse-temurin-25-noble`) before blaming the CI.
- Tests that import people (`massCreation`) delete what they created in `@AfterEach`; the other integration tests assume the seeded people are the only ones present.

## Runtime setup

- MySQL at `localhost:3306`, schema `rest_with_spring_boot_erudio`, `root`/`admin123` (in `application.yml`). Flyway (`src/main/resources/db/migration/V1..V18`) runs at startup, with `ddl-auto: none`. Schema and seed changes go in a new `V<n>__*.sql`; never edit an applied one (the checksums are shared with the Java project, which can use the same database).
- SMTP credentials come from the env vars `EMAIL_USERNAME` / `EMAIL_PASSWORD` (Gmail SMTP). `email.subject` and `email.message` in `application.yml` are the fallback subject and message (`EmailDefaultsConfig`), used only for the fields a request leaves out or blank.
- `FileStorageService` creates `file.upload-dir` (`/Code/UploadDir`) in its constructor at startup. On Windows this resolves against the current drive root.
- Swagger UI is served at `/swagger-ui/index.html`.
- Seeded login used by the tests: `leandro` / `admin123`.
- PDF export compiles the `.jrxml` templates at request time. That only works when the classes are on a normal classpath (`mvn spring-boot:run`, an IDE, or an exploded jar); from `java -jar` on the fat jar it fails (`javac` cannot read the nested jars, `PdfExporter` passes the `books.jasper` sub-report as a file path, and `people.jrxml` asks for the `Arial` font which a Linux JVM usually lacks). The `Dockerfile` works around all three. Export failures reach the client as an empty `403`, not a `500`.

## Architecture

Standard layering: `controllers` → `services` → `repository` (Spring Data JPA) → `model` entities. A few cross-cutting pieces span several files:

- **Entity ↔ DTO mapping**: services never expose entities. They convert with `DozerMapper.parseObject(...)` in `mapper/` (a Dozer wrapper, the equivalent of the Java `ObjectMapper` class). DTOs extend HATEOAS `RepresentationModel`, and services add links in a private `addHateoasLinks(dto)` using `linkTo(methodOn(XController::class.java)...)`. Adding or renaming a controller method means updating those links too. Paged results go through `PagedResourcesAssembler`.
- **Kotlin and `methodOn`**: `methodOn` creates a CGLIB proxy of the controller and of the return type of the method it calls, and Kotlin classes are final. Controllers are opened by the `spring` all-open preset; `BookDTO` and `PersonDTO` (data classes) are opened through the extra `all-open:annotation=org.springframework.hateoas.server.core.Relation` option in `pom.xml`. A new DTO returned by a controller method that is used in `methodOn` needs `@Relation` (or another opened annotation), otherwise the endpoint fails with "Could not generate CGLIB subclass". `PersonController.exportPage` takes `HttpServletRequest?` only so the link builder can pass `null`.
- **Nullability mirrors Java**: entity and DTO properties are nullable (`String?`, `Long? = null`, `Boolean?`) with default values so every class keeps a no-arg constructor for Dozer, Jackson and Hibernate. Do not turn them into non-null types: a null `title` must still fail Hibernate's not-null check instead of being stored as `""`.
- **OpenAPI docs live in interfaces**: each controller implements a `controllers/docs/*ControllerDocs` interface that carries the springdoc annotations. Put doc annotations there, not on the controller.
- **Content negotiation**: endpoints produce JSON, XML and YAML (`WebConfig.configureContentNegotiation`, `tools.jackson.dataformat` xml and yaml). The `withjson/`, `withxml/` and `withyaml/` integration-test packages mirror each other, so an endpoint change usually needs all three updated. The YAML converter is built in `config/JacksonConfig`.
- **Import/export strategy factories**: `FileExporterFactory` picks `CsvExporter` / `XlsxExporter` / `PdfExporter` from the request's `Accept` header (constants in `file/exporter/MediaTypes`). `FileImporterFactory` picks `CsvImporter` / `XlsxImporter` from the filename extension. Both look the implementation up as a bean via `ApplicationContext`. To add a format, add a `@Component` implementation and a branch in the factory. `PersonController.exportPage` also maps Accept type to file extension itself. `PersonService.exportPage/exportPerson` wrap every failure (including the factory's `BadRequestException`) in `RuntimeException("Error during file export!")`, and `massCreation` turns every failure after the empty-file check into `FileStorageException("Error processing the file!")`; both end as a 500 on purpose.
- **PDF export**: `person.jrxml` embeds the `books` subreport and a QR code from `QRCodeService` (ZXing); templates are in `src/main/resources/templates/`. `PersonDTO.name` (read by `people.jrxml` and `person.jrxml`) is a computed property. A "not found" export asked with `Accept: application/pdf` only cannot render its JSON error and ends as an empty 403; add `application/json` to the `Accept` header to see the 404.
- **E-mail**: `EmailController` → `EmailService` → `EmailSender` (builds the MIME message; sender address is `spring.mail.username`). The request's `body` is the HTML message. For `/withAttachment` the upload is copied to a temporary file that is deleted afterwards, and the recipient sees the uploaded file's original name. The `emailRequest` part is parsed with `JsonMapper.builderWithJackson2Defaults()`, so unknown fields are rejected. `EmailSender` is a singleton holding the state of the message being built, so it is not safe for concurrent requests.
- **Security** (`config/SecurityConfig`, `security/jwt/*`): stateless JWT via auth0 `java-jwt`, with `JwtTokenFilter` added before `UsernamePasswordAuthenticationFilter`. `/auth/signin`, `/auth/refresh/**`, `/auth/createUser`, `/swagger-ui/**` and `/v3/api-docs/**` are public. `/api/**` requires authentication and `/users` is denied. `UserService` is the `UserDetailsService`, and `model/User` is the `UserDetails` (its `password` is a private property with hand-written `getPassword()`/`setPassword()`, because a public property would clash with the `UserDetails` getter). Passwords are PBKDF2 (custom params in `SecurityConfig.passwordEncoder`). `generateHashedPassword()` in `Startup.kt` is an unused helper that prints hashes for seeding users in migrations.
- **CORS** origins come from `cors.originPatterns` in `application.yml`, applied in `WebConfig`. `PersonControllerCorsTest` checks it.
- **Error handling**: `exception/handler/CustomEntityResponseHandler` maps custom exceptions to HTTP statuses. Anything unmapped becomes a 500 with an `ExceptionResponse` body.
- **Dependency injection** is field `@Autowired private lateinit var`, which is what the Mockito `@InjectMocks` unit tests and `ReflectionTestUtils.setField(..., "fieldName", ...)` rely on: keep the field names (`context`, `service`, `emailSender`, `emailConfigs`, `emailDefaults`).
- **Logging** uses `java.util.logging.Logger` (bridged to Logback by Spring Boot), as in the other Kotlin projects of the course.

## Settings that preserve the Java behavior

- `src/test/resources/application.yml` is a full copy of the main one, not an overlay. Any change to the settings below must be made in both files, otherwise the tests run a different configuration from production.
- `spring.jackson.use-jackson2-defaults: true` keeps the JSON/XML output as in the Java project: declaration order instead of Jackson 3's alphabetical order, and dates as `+00:00` instead of `Z`.
- `config/JacksonConfig` covers what that flag does not: it moves the HATEOAS `links` after the DTO's own properties (`CollectionModel` is excluded because it always had `links` first), and it builds the YAML converter (declaration order, dates as epoch milliseconds, unknown properties ignored). Spring Boot only configures the JSON and XML mappers.
- `CustomEntityResponseHandler.createResponseEntity` sets `type: about:blank` on every `ProblemDetail`, because Spring Framework 7 no longer defaults it and the field would disappear from the error payloads.
- `spring.jpa.properties.hibernate.check_nullability: true` is required. springdoc 3 pulls in Bean Validation, and Hibernate then silently turns its own not-null check off.
- `springdoc.api-docs.version: openapi_3_0` keeps the OpenAPI 3.0 document (springdoc 3 defaults to 3.1).
- `springdoc.enable-kotlin: false` keeps the generated OpenAPI document identical to the Java project's. Without it springdoc adds `nullable: true` to every property that is a Kotlin `?` type. Only the API title/description (Kotlin instead of Java) differ from the Java document.
- Boot 4 split its auto-configuration into modules, so the raw libraries no longer auto-configure. Keep `spring-boot-starter-flyway`, `spring-boot-starter-hateoas` and `spring-boot-starter-webmvc`.
- Kotlin compiler flags: `-Xjsr305=strict` and the `spring` (plus `all-open`) plugin in `pom.xml`. `jackson-module-kotlin` is `tools.jackson.module` (Jackson 3), not the `com.fasterxml` artifact.

## Dependency versions

Versions are inherited from the Spring Boot parent wherever it manages them (Kotlin, Flyway, Jackson, Hibernate, Testcontainers, JUnit, Mockito, the MySQL driver used by the Flyway plugin via `${mysql.version}`). Only libraries the BOM does not manage carry an explicit version property in `pom.xml`: springdoc, REST Assured, POI, commons-csv, JasperReports, ZXing, java-jwt, Dozer, GreenMail and mockito-kotlin.

## Docker and CI

- `Dockerfile` (`eclipse-temurin:25-jdk`) does not run `java -jar`: it unpacks `target/*.jar` into `/app`, adds `/app/config/jasperreports.properties` (ignore missing font) and starts `br.com.erudio.StartupKt` (the class Kotlin generates for the top-level `main` in `Startup.kt`) with `/app/config:/app/BOOT-INF/classes:/app/BOOT-INF/lib/*` as classpath, so the three PDF problems above do not happen. The unpacking uses a BuildKit bind mount of `target/`, so `.dockerignore` must keep `!target/*.jar`.
- `docker-compose.yml`: `db` is `mysql:9` (host port 3308, since the developer's own MySQL owns 3306) with a healthcheck, and `app` waits for it (`condition: service_healthy`). The image is `leandrocgsi/rest-with-spring-boot-kotlin-erudio`, deliberately different from the Java project's `rest-with-spring-boot-erudio` so the two never overwrite each other on Docker Hub. E-mail credentials are not in the compose file; add `EMAIL_USERNAME` / `EMAIL_PASSWORD` to the `app` environment if you need to send mail.
- `.github/workflows/continuous-deployment.yml` runs on push to `main`: Docker Hub login, Java 25 (temurin) setup, `mvn clean package` (runs all tests; the integration tests need Docker, which `ubuntu-latest` has), `docker compose build`, then pushes the image tagged `latest` and `${{ github.run_id }}`. It needs the repository secrets `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`.
- `.claude/` (project skills and local settings) is git-ignored on purpose.

## Notes

- The owner dislikes comments: do not add code comments (Kotlin, XML, YAML, Dockerfile, ignore files); put explanations in this file or in the commit message instead.
- `spring-boot-devtools` is on the runtime classpath.
- `TestLogController` (`/api/test/v1`) is a demo endpoint for exercising log levels.
