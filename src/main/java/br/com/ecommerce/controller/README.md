# Módulo: controller (`br.com.ecommerce.controller`)

## 1. Objetivo do Módulo
Disponibilizar os endpoints da API RESTful do e-commerce, atuando como a camada de entrada do protocolo HTTP. Recebe as requisições dos clientes, valida os parâmetros de entrada, extrai o contexto de segurança e delega a execução para a camada de serviços correspondente.

---

## 2. Responsabilidade Principal
- Mapear rotas, métodos HTTP (`GET`, `POST`, `PUT`, `DELETE`) e códigos de status HTTP (`200 OK`, `201 Created`, `204 No Content`).
- Disparar validação sintática nos corpos de requisição através da anotação `@Valid`.
- Injetar os dados do usuário autenticado a partir do token JWT via `@AuthenticationPrincipal Jwt`.
- Aplicar regras de autorização granulares baseadas em perfil (`@PreAuthorize("hasRole('ADMIN')")`).
- Encapsular as respostas no formato de envelope padrão `{"data": ...}`.

---

## 3. Funcionalidades Existentes

### 3.1. `AuthController` (`/api/v1/auth`)
- `POST /register`: Cadastro de novo usuário. Retorna status `201 Created` e payload com dados do usuário e tokens.
- `POST /login`: Autenticação por e-mail e senha. Retorna tokens de acesso e refresh.

### 3.2. `CatalogController` (`/api/v1`)
- `GET /categories`: Lista todas as categorias cadastradas (público).
- `GET /categories/{id}`: Detalha categoria específica (público).
- `POST /categories`: Criação de categoria (`ADMIN`, `201 Created`).
- `PUT /categories/{id}`: Atualização de categoria (`ADMIN`).
- `DELETE /categories/{id}`: Exclusão de categoria (`ADMIN`, `204 No Content`).
- `GET /products`: Listagem paginada de produtos com filtros opcionais (`page`, `limit`, `search`, `categoryId`) (público).
- `GET /products/{id}`: Detalha produto específico (público).
- `POST /products`: Cadastro de produto (`ADMIN`, `201 Created`).
- `PUT /products/{id}`: Atualização de produto (`ADMIN`).
- `DELETE /products/{id}`: Exclusão de produto (`ADMIN`, `204 No Content`).

### 3.3. `CartController` (`/api/v1/cart`)
- `GET /`: Consulta os itens e valor total do carrinho do usuário autenticado.
- `POST /items`: Adiciona item ao carrinho (payload: `productId`, `quantity`).
- `DELETE /items/{itemId}`: Remove item do carrinho por ID do produto.

### 3.4. `OrderController` (`/api/v1/orders`)
- `GET /`: Lista o histórico de pedidos do usuário autenticado.
- `POST /`: Dispara o checkout dos itens contidos no carrinho, gerando o pedido (`201 Created`).

### 3.5. `PaymentController` (`/api/v1/payments`)
- `POST /`: Cria registro de pagamento para um pedido (`orderId`) informado (`201 Created`).
- `POST /{id}/confirm`: Confirma o pagamento; aceita cabeçalho opcional/requerido `Idempotency-Key`.
- `POST /{id}/cancel`: Cancela o pagamento e o pedido associado.

### 3.6. `UserController` (`/api/v1/users`)
- `GET /{id}`: Consulta dados de usuário (autorizado apenas para o próprio usuário ou perfil `ADMIN`).
- `PUT /{id}`: Atualiza dados de cadastro (nome, e-mail, senha).
- `DELETE /{id}`: Exclui a conta do usuário (`204 No Content`).

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.service.*`: Injeção de dependência via construtor de todos os serviços de domínio (`AuthService`, `CatalogService`, `CartService`, `OrderService`, `PaymentService`, `UserService`).
- `br.com.ecommerce.service.CatalogService.*`: Records de entrada (`CategoryInput`, `ProductInput`).
- `br.com.ecommerce.service.CartService.*`: Record `AddItem`.
- `br.com.ecommerce.service.AuthService.*`: Records `Register` e `Login`.
- `br.com.ecommerce.service.UserService.*`: Record `UpdateUser`.

### Dependências Externas
- `org.springframework.web.bind.annotation.*`: Anotações do Spring MVC (`@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, `@ResponseStatus`).
- `org.springframework.security.core.annotation.AuthenticationPrincipal`: Injeção do principal autenticado.
- `org.springframework.security.oauth2.jwt.Jwt`: Objeto representativo do token JWT decodificado.
- `org.springframework.security.access.prepost.PreAuthorize`: Anotação de segurança de método para regras RBAC.
- `jakarta.validation.Valid`: Gatilho de validação do Hibernate Validator / Jakarta Validation.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.config`: Estabelece o pipeline de segurança que protege ou libera os endpoints deste módulo.
- `br.com.ecommerce.service`: Destino exclusivo de todas as chamadas efetuadas pelos controladores.

---

## 6. Pontos de Entrada
Todos os métodos anotados com `@GetMapping`, `@PostMapping`, `@PutMapping` e `@DeleteMapping` nos 6 controladores constituem os pontos de entrada públicos e autenticados de toda a aplicação web.

---

## 7. Fluxos Importantes
- **Fluxo de Autenticação de Contexto**: O controlador recebe a requisição com o token Bearer validado pelo Spring Security, injeta a instância de `Jwt` no parâmetro `@AuthenticationPrincipal Jwt jwt` e repassa `jwt.getSubject()` (que contém o UUID do usuário) para o serviço de negócio.

---

## 8. Arquivos Críticos
- `CatalogController.java`: Centraliza o maior volume de endpoints da aplicação (categorias e produtos), combinando acessos públicos com operações restritas a administradores.
- `OrderController.java`: Ponto de transição do carrinho de compras para o fechamento definitivo da venda.
- `PaymentController.java`: Gerencia o ciclo financeiro e orquestra a confirmação com chave de idempotência.

---

## 9. Observações Técnicas e Débitos Identificados
- **Retorno Genérico `Object`**: Todos os controladores utilizam `public Object metodo(...)` retornando instâncias de `Map.of("data", ...)`. Isso remove a segurança de tipos do Java, impede geração automatizada de documentação OpenAPI/Swagger fiel e dificulta a evolução em contratos orientados a especificações (Spec-Driven Development).
  > [!TIP]
  > Substituir por classes DTO tipadas, tais como `ResponseEntity<ApiResponse<OrderResponseDTO>>`.
- **Registro DTO Acoplado ao Controller**: `PaymentController` declara o record `CreatePayment` internamente no arquivo do controlador, enquanto outros controladores utilizam records definidos dentro dos Services. Recomenda-se criar um pacote padronizado `br.com.ecommerce.dto` para isolar contratos de transferência.
- **Cabeçalho Idempotency-Key Opcional no Mapeamento**: No método `PaymentController.confirm`, o parâmetro está mapeado como `@RequestHeader(value = "Idempotency-Key", required = false) String key`, delegando a obrigatoriedade para a validação imperativa dentro de `PaymentService.java`.
