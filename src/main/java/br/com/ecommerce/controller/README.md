# Módulo: controller (`br.com.ecommerce.controller`)

## 1. Objetivo do Módulo
Disponibilizar os endpoints da API RESTful do e-commerce, atuando como a camada de entrada do protocolo HTTP. Recebe as requisições dos clientes, valida os parâmetros de entrada, extrai o contexto de segurança e delega a execução para a camada de serviços correspondente, aplicando regras de autorização granulares (RBAC).

---

## 2. Responsabilidade Principal
- Mapear rotas, métodos HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) e códigos de status HTTP (`200 OK`, `201 Created`, `204 No Content`).
- Disparar validação sintática nos corpos de requisição através da anotação `@Valid`.
- Injetar os dados do usuário autenticado a partir do token JWT via `@AuthenticationPrincipal Jwt`.
- Aplicar regras de autorização baseadas em perfil (`@PreAuthorize("hasRole('ADMIN')")`).
- Encapsular as respostas no formato de envelope padrão `{"data": ...}`.

---

## 3. Funcionalidades Existentes e Endpoints

### 3.1. `AuthController` (`/api/v1/auth`)
- `POST /register`: Cadastro de novo usuário (`201 Created`). Emite dados do usuário e tokens JWT.
- `POST /login`: Autenticação por e-mail e senha. Emite par de tokens (acesso e refresh).

### 3.2. `UserController` (`/api/v1/users`)
- `GET /{id}`: Consulta dados de usuário (restrito ao próprio usuário ou perfil `ADMIN`).
- `PUT /{id}`: Atualiza dados cadastrais básicos (nome, e-mail).
- `PUT /{id}/password`: Permite ao usuário logado alterar a sua própria senha de acesso.
- `DELETE /{id}`: **Exclusivo para `ADMIN`** (`@PreAuthorize("hasRole('ADMIN')")`). O usuário comum não possui permissão de auto-exclusão de conta.

### 3.3. `CatalogController` (`/api/v1`)
- `GET /categories`: Lista todas as categorias na árvore hierárquica de profundidade ilimitada (público).
- `GET /categories/{id}`: Detalha categoria específica (público).
- `POST /categories`: Criação de categoria (`ADMIN`, `201 Created`).
- `PUT /categories/{id}`: Atualização de categoria (`ADMIN`).
- `DELETE /categories/{id}`: Exclusão de categoria (`ADMIN`, `204 No Content`).
- `GET /products`: Listagem paginada de produtos ativos. **Produtos com status `INACTIVE` são excluídos imediatamente da resposta pública**.
- `GET /products/{id}`: Detalha produto e suas múltiplas categorias associadas (público).
- `POST /products`: Cadastro de produto (`ADMIN`, `201 Created`), recebendo lista de IDs de categorias (`categoryIds`).
- `PUT /products/{id}`: Atualização de atributos ou categorias do produto (`ADMIN`).
- `DELETE /products/{id}`: Exclusão de produto (`ADMIN`, `204 No Content`).

### 3.4. `CartController` (`/api/v1/cart`)
- `GET /`: Consulta os itens e valor total do carrinho do usuário autenticado.
- `POST /items`: Adiciona produto ao carrinho (payload: `productId`, `quantity`).
- `DELETE /items/{itemId}`: Remove item do carrinho por ID do produto.

### 3.5. `OrderController` (`/order`, `/api/v1/orders`)
- `GET /`: Lista todos os pedidos do usuário autenticado de forma direta (sem filtro).
  > [!NOTE]
  > O administrador não possui rota para filtrar ou listar pedidos de um usuário específico.
- `POST /`: Checkout com recebimento de dados de entrega (`deliveryAddress`), debitando estoque e gerando pedido em status `PENDING` (`201 Created`).
- `PATCH /{id}/status`: **Exclusivo `ADMIN`**. Atualiza o status logístico para `PACKING` ou `SHIPPED`.
- `PATCH /{id}/delivered`: Permite ao próprio usuário titular confirmar o recebimento do pedido, ou ao `ADMIN`.
- `DELETE /{id}`: Cancela o pedido integralmente seguindo o padrão RESTful (`DELETE /order/{id}`). Executa estorno atômico de estoque se o status for `PENDING`, `PAID` ou `PACKING`. Rejeita cancelamento com erro `400 Bad Request` se o status for `SHIPPED` ou `DELIVERED`. Retorna `404 Not Found` se o pedido não existir e `403 Forbidden` caso um usuário não-titular tente cancelar sem papel `ADMIN`. *(O endpoint anterior `POST /{id}/cancel` foi desativado).*

### 3.6. `PaymentController` (`/api/v1/payments`)
- `POST /`: Cria registro de pagamento para um pedido (`orderId`) informado (`201 Created`).
- `POST /{id}/confirm`: **Invocado ativamente pelo front-end** para confirmar a liquidação. Exige cabeçalho `Idempotency-Key` e altera o pedido para `OrderStatus.PAID`.
- `POST /{id}/cancel`: Cancela o pagamento e aciona o cancelamento do pedido associado (respeitando a regra de bloqueio se já enviado).

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.service.*`: Injeção de dependência de todos os serviços de domínio (`AuthService`, `CatalogService`, `CartService`, `OrderService`, `PaymentService`, `UserService`).
- Records de contrato de entrada (`CategoryInput`, `ProductInput`, `AddItem`, `Register`, `Login`, `CheckoutInput`, `ChangePasswordInput`).

### Dependências Externas
- `org.springframework.web.bind.annotation.*`: Anotações do Spring MVC (`@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, `@ResponseStatus`, `@PatchMapping`).
- `org.springframework.security.core.annotation.AuthenticationPrincipal`: Injeção do principal autenticado.
- `org.springframework.security.oauth2.jwt.Jwt`: Objeto representativo do token JWT decodificado.
- `org.springframework.security.access.prepost.PreAuthorize`: Anotação de segurança de método para regras RBAC.
- `jakarta.validation.Valid`: Gatilho de validação do Hibernate Validator / Jakarta Validation.

---

## 5. Regras Críticas de Autorização

1. **Auto-Exclusão Proibida**:
   - `DELETE /api/v1/users/{id}`: Requer obrigatoriamente a role `ADMIN`. Se um cliente comum tentar invocar este endpoint passando o próprio ID, o Spring Security retornará `403 Forbidden`.
2. **Atualização Logística Restrita**:
   - Transições para `PACKING` e `SHIPPED` são permitidas apenas para administradores.
3. **Confirmação de Entrega pelo Consumidor**:
   - `PATCH /api/v1/orders/{id}/delivered` valida que o requisitante autenticado é o titular do pedido ou possui perfil `ADMIN`.

---

## 6. Observações Técnicas e Débitos Identificados
- **Retorno Genérico `Object`**: Os controladores utilizam `Map.of("data", ...)` com retorno `Object`. Recomenda-se migrar para DTOs tipados (`ResponseEntity<ApiResponse<T>>`) para permitir a geração de especificações OpenAPI precisas.
- **Isolamento de Contratos de Entrada**: Mover os Records de DTO de dentro dos Services para um pacote dedicado `br.com.ecommerce.dto`.
