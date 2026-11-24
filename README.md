# API E-commerce — Java 21 + Spring Boot 3 (Sem Docker)

Sistema de e-commerce construído sobre Spring Boot 3.5.6 e Java 21, organizado segundo a arquitetura Monólito em Camadas (Controller, Service, Repository, Model). O projeto opera de forma 100% autônoma e local com banco de dados embutido H2 e carrinho em memória, sem qualquer dependência de Docker ou containers.

---

## Pré-requisitos
- **Java**: JDK 21 (LTS).
- **Maven**: 3.9+ instalado ou via CLI.
- **Node.js**: (Opcional) v20+ caso deseje executar a suíte Postman via CLI com Newman.

> [!IMPORTANT]
> **Execução Sem Docker**: Este projeto foi projetado para execução local pura. Não utilize Docker, Docker Compose ou Testcontainers. Consulte o guia oficial [`docs/AMBIENTE_LOCAL.md`](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/AMBIENTE_LOCAL.md) para detalhes completos de infraestrutura local.

---

## Execução Local

1. **Iniciar a Aplicação:**
   ```powershell
   mvn spring-boot:run
   ```
   A API estará disponível em: `http://localhost:8080` (console H2 disponível em `http://localhost:8080/h2-console`).

2. **No IntelliJ IDEA / VS Code / Eclipse:**
   - Abra a pasta do projeto.
   - Aguarde o Maven sincronizar as dependências.
   - Execute a classe principal `br.com.ecommerce.EcommerceApplication`.

---

## Execução dos Testes Automatizados

- **Executar todos os testes automatizados (unitários e de integração):**
  ```powershell
  mvn clean verify
  ```
- **Executar testes do Postman (Newman) localmente:**
  ```powershell
  npx --yes newman run postman/order-cancellation.postman_collection.json -e postman/ecommerce-local.postman_environment.json
  ```

---

## Endpoints da API

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Cadastro de novos clientes |
| POST | `/api/v1/auth/login` | Público | Autenticação por email e senha |
| GET | `/api/v1/categories` | Público | Listagem de categorias |
| GET | `/api/v1/categories/{id}` | Público | Detalhamento de categoria |
| POST, PUT, DELETE | `/api/v1/categories[/{id}]` | ADMIN | Gestão do catálogo de categorias |
| GET | `/api/v1/products` | Público | Listagem de produtos ativos |
| GET | `/api/v1/products/{id}` | Público | Detalhamento de produto |
| POST, PUT, DELETE | `/api/v1/products[/{id}]` | ADMIN | Gestão do catálogo de produtos |
| GET, POST | `/api/v1/cart`, `/api/v1/cart/items` | Autenticado | Gestão do carrinho em memória |
| DELETE | `/api/v1/cart/items/{itemId}` | Autenticado | Remoção de item do carrinho |
| GET, POST | `/order`, `/api/v1/orders` | Autenticado | Listagem e checkout de pedidos |
| **DELETE** | **`/order/{id}`** | **Titular / ADMIN** | **Cancelamento total de pedido e estorno condicional de estoque** |
| POST | `/api/v1/payments` | Autenticado | Criação de intenção de pagamento |
| POST | `/api/v1/payments/{id}/confirm` | Autenticado | Confirmação de pagamento |
| POST | `/api/v1/payments/{id}/cancel` | Autenticado | Cancelamento de pagamento e sincronização do pedido |
| GET, PUT, DELETE | `/api/v1/users/{id}` | Próprio / ADMIN | Gestão de contas de usuários |

---

## Documentação Técnica Oficial

1. **Execução e Ambiente Local**:
   - [Guia de Ambiente Local e Sem Docker (`docs/AMBIENTE_LOCAL.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/AMBIENTE_LOCAL.md)
   - [Instruções para Agentes de IA (`AGENTS.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/AGENTS.md)

2. **Visão Global & Arquitetura**:
   - [Arquitetura do Sistema (`docs/arquitetura-do-sistema.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/arquitetura-do-sistema.md)
   - [Objetivo do Sistema (`docs/objetivo-do-sistema.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/objetivo-do-sistema.md)
   - [Especificação de Cancelamento de Pedido (`docs/specs/001-order-cancellation-stock-restoration.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/specs/001-order-cancellation-stock-restoration.md)

3. **Documentação por Camada / Módulo**:
   - [Controllers REST (`src/main/java/br/com/ecommerce/controller/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/controller/README.md)
   - [Lógica de Negócios & Serviços (`src/main/java/br/com/ecommerce/service/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/service/README.md)
   - [Repositórios & Persistência (`src/main/java/br/com/ecommerce/repository/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/repository/README.md)
   - [Modelos & Entidades JPA (`src/main/java/br/com/ecommerce/model/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/java/br/com/ecommerce/model/README.md)
   - [Configurações de Recursos (`src/main/resources/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/main/resources/README.md)
   - [Módulo de Testes (`src/test/java/br/com/ecommerce/README.md`)](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/src/test/java/br/com/ecommerce/README.md)
