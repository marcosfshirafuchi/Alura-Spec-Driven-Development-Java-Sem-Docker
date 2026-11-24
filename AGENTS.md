# Instruções para Agentes de IA — Projeto E-Commerce Spring Boot (Sem Docker)

Este documento estabelece as diretrizes obrigatórias e imutáveis para todos os agentes de IA que operarem neste repositório.

---

## 1. Diretriz Primária: Execução 100% Local Sem Docker

> [!CAUTION]
> **É EXPRESSAMENTE PROIBIDO O USO DE DOCKER EM QUALQUER CIRCUNSTÂNCIA.**
> - Não execute comandos `docker run`, `docker compose`, `docker build`.
> - Não adicione bibliotecas ou anotações de Testcontainers (`@Testcontainers`, `@Container`).
> - Não exija a presença de Docker Desktop ou daemon do Docker.
> - Não crie containers para bancos de dados, redis ou filas.

O projeto é estritamente **autônomo e executável localmente**:
- **Java**: Versão 21.
- **Framework**: Spring Boot 3.5.6.
- **Banco de Dados**: H2 Database embutido (`file:./data/ecommerce` para dev, `mem:ecommerce_test` para testes).
- **Gerenciador de Build**: Apache Maven (`mvn`).
- **Carrinho de Compras**: Mantido em memória (`ConcurrentHashMap`) no `CartService`.

---

## 2. Documentação Global Obrigatória

Antes de iniciar qualquer tarefa ou alteração de ambiente:
1. **Consulte obrigatoriamente** o guia [`docs/AMBIENTE_LOCAL.md`](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/docs/AMBIENTE_LOCAL.md).
2. Toda e qualquer nova configuração, ajuste de porta, variáveis de ambiente ou correção de problemas identificados **DEVE ser persistida** em `docs/AMBIENTE_LOCAL.md`.
3. Nunca invente dependências ou arquiteturas ausentes no código existente.

---

## 3. Padrão de Endpoints RESTful de Pedidos

- **Cancelamento de Pedidos**: Mapeado obrigatoriamente como `DELETE /order/{id}` (com compatibilidade com `/api/v1/orders/{id}`).
- **O endpoint anterior `POST /order/{id}/cancel` está permanentemente desativado** e deve retornar `404 Not Found`.
- Códigos HTTP obrigatórios para cancelamento:
  - `200 OK`: Cancelado com sucesso com envelope `{"data": PurchaseOrder}` e status `CANCELED`.
  - `400 Bad Request`: Rejeição ao tentar cancelar pedidos em status `SHIPPED` ou `DELIVERED`.
  - `401 Unauthorized`: Requisições sem token JWT válido.
  - `403 Forbidden`: Requisições feitas por usuários que não são o titular do pedido e não possuem o papel `ROLE_ADMIN`.
  - `404 Not Found`: Pedido inexistente ou endpoint antigo `/cancel`.

---

## 4. Procedimentos de Validação e Testes Obrigatórios

1. **Testes Automatizados (Maven)**:
   Sempre valide a compilação e a suíte completa de testes antes de considerar qualquer tarefa concluída:
   ```powershell
   mvn clean verify
   ```
2. **Testes de Caixa Preta (Postman / Newman)**:
   Valide as collections do Postman executando o Newman localmente no host (sem containers):
   ```powershell
   npx --yes newman run postman/order-cancellation.postman_collection.json -e postman/ecommerce-local.postman_environment.json
   ```
3. **Não Declare Sucesso Sem Evidência Real**:
   Sempre apresente a contagem de testes executados, aprovados e o log de sucesso real do Maven e Newman.
