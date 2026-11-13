# API E-commerce — Java 21 + Spring Boot 3

Conversão do projeto Node.js/Express/Prisma fornecido, organizada em camadas Controller, Service, Repository e Entity.

## Pré-requisitos
- JDK 21, Maven 3.9+, Docker e Docker Compose.

## Executar
1. `docker compose up -d` (PostgreSQL e Redis)
2. `mvn spring-boot:run`
3. API disponível em `http://localhost:8080/api/v1`.

No IntelliJ IDEA: **File > Open**, escolha a pasta, importe o Maven e execute `EcommerceApplication`.

## Endpoints
| Método | Endpoint | Acesso |
|---|---|---|
| POST | /api/v1/auth/register | Público |
| POST | /api/v1/auth/login | Público |
| GET | /api/v1/categories | Público |
| GET | /api/v1/categories/{id} | Público |
| POST, PUT, DELETE | /api/v1/categories[/{id}] | ADMIN |
| GET | /api/v1/products | Público |
| GET | /api/v1/products/{id} | Público |
| POST, PUT, DELETE | /api/v1/products[/{id}] | ADMIN |
| GET, POST | /api/v1/cart, /api/v1/cart/items | Autenticado |
| DELETE | /api/v1/cart/items/{itemId} | Autenticado |
| GET, POST | /api/v1/orders | Autenticado |
| POST | /api/v1/payments | Autenticado |
| POST | /api/v1/payments/{id}/confirm | Autenticado |
| POST | /api/v1/payments/{id}/cancel | Autenticado |
| GET, PUT, DELETE | /api/v1/users/{id} | Próprio usuário ou ADMIN |

## Teste inicial
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
 -H 'Content-Type: application/json' \
 -d '{"name":"Teste","email":"teste@example.com","password":"123456"}'
```
Use `Authorization: Bearer <accessToken>` nas rotas protegidas. Para confirmar pagamento, envie também `Idempotency-Key: <valor>`.

## Observações importantes
- Todos os usuários criados pela API recebem papel USER. Para criar um ADMIN de desenvolvimento, altere o papel diretamente no banco em ambiente local.
- JWT de acesso expira em 15 minutos. Refresh token é armazenado no Redis por 7 dias, mas não há endpoint de renovação no projeto original nem nesta migração.
- `ddl-auto: update` e credenciais do Compose são **somente para desenvolvimento**.
- O pagamento é simulado: não há integração real com gateway.
- A listagem de produtos filtra em memória nesta primeira versão. Para produção, implemente filtros/paginação no banco.
- Esta é uma conversão funcional de referência, **não validada por testes de integração**. Antes de produção, adicionar migrations, testes de concorrência/estoque, observabilidade e regras de multi-tenancy.

---

## Documentação Técnica Oficial (Spec-Driven Development)

Para análise aprofundada da arquitetura, objetivos de negócio, contratos de API e documentação técnica por camada:

1. **Visão Global**:
   - [Arquitetura do Sistema](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/arquitetura-do-sistema.md)
   - [Objetivo do Sistema](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/objetivo-do-sistema.md)

2. **Documentação por Camada / Módulo**:
   - [Config & Segurança (br.com.ecommerce.config)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/config/README.md)
   - [Controllers REST (br.com.ecommerce.controller)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/controller/README.md)
   - [Lógica de Negócios & Serviços (br.com.ecommerce.service)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/service/README.md)
   - [Repositórios & Persistência (br.com.ecommerce.repository)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/repository/README.md)
   - [Modelos & Entidades JPA (br.com.ecommerce.model)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/model/README.md)
   - [Configurações de Recursos (src/main/resources)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/resources/README.md)
   - [Módulo de Testes (src/test/java/br/com/ecommerce)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/test/java/br/com/ecommerce/README.md)

