# Postman: collection, environment e testes automatizados

| Arquivo | O que é |
|---|---|
| `Formação Spring Boot com Kotlin - ... .postman_collection.json` | 51 requisições (Auth, person, book, file, e-Mail), cada uma com testes (`pm.test`) |
| `SPRING_BOOT_KOTLIN_ERUDIO.postman_environment.json` | Variáveis usadas pela collection |
| `files/` | Arquivos de exemplo usados nos uploads (`sample.txt`, `people.csv`, `people.xlsx`) |

## Usando no Postman

1. Importe a collection e o environment e selecione o environment `SPRING_BOOT_KOTLIN_ERUDIO`.
2. Os uploads usam caminhos relativos (`files/...`): em **Settings → General → Location**, aponte o *Working directory* para esta pasta (`Collections`).
3. Suba a API (`docker compose up -d --build` publica em `http://localhost:8080`) e rode a collection inteira no Runner, ou qualquer requisição isolada. Um script no nível da collection faz o signin sozinho quando não há token válido, então a ordem não importa; a ordem das pastas só serve para o Runner.

## Variáveis do environment

| Variável | Uso |
|---|---|
| `baseUrl` | Endereço da API |
| `username` / `password` | Usuário do seed (`leandro` / `admin123`) usado no signin |
| `accessToken` / `refreshToken` / `tokenExpiration` | Preenchidas pelo signin e pelo refresh |
| `newUsername` / `newPassword` | Usuário criado por `Create new Users` (o nome é gerado a cada execução) |
| `personId` / `bookId` | Preenchidas pelos POST e usadas por PUT, PATCH, DELETE e pelos GET de "deleted" |
| `sendEmails` | `true` liga os dois testes de e-mail que enviam de verdade (precisam de SMTP configurado na API); por padrão são pulados |
| `emailTo` | Destinatário desses e-mails |

## Rodando pela linha de comando (newman)

```bash
npx newman run Collections/*.postman_collection.json \
  -e Collections/SPRING_BOOT_KOTLIN_ERUDIO.postman_environment.json \
  --working-dir Collections
```

Use `--env-var baseUrl=http://localhost:8085` para outra porta e `--folder person` para uma pasta só.

O mesmo comando roda no GitHub Actions (`.github/workflows/continuous-deployment.yml`): depois do `mvn clean package`, o workflow sobe o `docker compose`, espera a API responder e executa o newman antes de publicar a imagem. Se um teste da collection falhar, a imagem não é publicada.

## O que os testes verificam

- **Auth:** tokens e JWT (`sub` e `roles`), credenciais inválidas e em branco, criação de usuário com senha em hash e signin do usuário criado, refresh, refresh com token inválido e refresh de usuário inexistente.
- **person e book:** CRUD completo (POST, GET, PUT, PATCH, DELETE), corpo, links HATEOAS/HAL, paginação, busca por nome, 404 depois do delete, JSON malformado (400), sem token ou com token inválido (403), livro sem título rejeitado.
- **Negociação de conteúdo:** JSON, XML e YAML.
- **Relatórios:** XLSX, CSV e PDF (cabeçalho do arquivo e `Content-Disposition`).
- **Importação em massa:** XLSX e CSV, e arquivo não suportado. Os testes apagam o que criaram.
- **file:** upload, upload múltiplo, download com o mesmo conteúdo, arquivo inexistente e upload sem token.
- **CORS:** origem permitida e origem bloqueada.
- **e-Mail:** destinatário inválido, JSON malformado e content type errado (sem precisar de SMTP).

## Observações

- Os relatórios em PDF baixam imagens de `raw.githubusercontent.com`; sem internet esses testes falham. A geração do PDF com 70 pessoas leva alguns segundos, por isso o limite de tempo global é de 30 s.
- Cada execução cria um usuário novo (não existe endpoint de exclusão de usuário).
