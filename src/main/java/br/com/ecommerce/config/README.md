# Módulo: config (`br.com.ecommerce.config`)

## 1. Objetivo do Módulo
Configurar os componentes transversais de infraestrutura da aplicação, especificamente o pipeline de segurança HTTP, autenticação stateless baseada em JSON Web Tokens (OAuth2 Resource Server) com RBAC granular e o tratamento centralizado de exceções na camada REST.

---

## 2. Responsabilidade Principal
- Definir regras de controle de acesso a rotas públicas e privadas via Spring Security (`SecurityFilterChain`).
- Proteger rotas sensíveis de administração (`ADMIN`) e garantir permissões de perfil para clientes (`USER`).
- Configurar decodificadores e codificadores JWT com criptografia simétrica HMAC-SHA256 (`NimbusJwtDecoder` e `NimbusJwtEncoder`).
- Mapear a claim customizada `role` do token para autoridades com prefixo `ROLE_`.
- Capturar exceções de negócio (`ApiException`), validações de payload (`MethodArgumentNotValidException`) e erros de integridade (`DataIntegrityViolationException`), transformando-os em respostas HTTP estruturadas.

---

## 3. Regras de Segurança e Matriz de Permissões HTTP

### 3.1. Endpoints Públicos (`permitAll()`)
- `/api/v1/auth/**`: Registro e login.
- `GET /api/v1/products/**`: Consulta de produtos (apenas `ACTIVE`).
- `GET /api/v1/categories/**`: Consulta da árvore de categorias.
- `/actuator/health`: Monitoramento de saúde da API.
- `/h2-console/**`: Console web local do banco de dados H2.

### 3.2. Endpoints Autenticados com Regras Específicas
- **Exclusão de Usuários (`DELETE /api/v1/users/**`)**: Restrita a `hasRole('ADMIN')`. Usuários comuns não possuem permissão de auto-exclusão.
- **Alteração de Senha (`PUT /api/v1/users/{id}/password`)**: Acessível pelo próprio usuário logado (`authenticated()`).
- **Atualização Logística de Pedidos (`PATCH /api/v1/orders/{id}/status`)**: Restrita a `hasRole('ADMIN')` para transição entre `PACKING` e `SHIPPED`.
- **Confirmação de Entrega (`PATCH /api/v1/orders/{id}/delivered`)**: Acessível ao cliente titular (`USER`) ou a um `ADMIN`.
- **Checkout e Carrinho (`/api/v1/cart/**`, `/api/v1/orders`)**: Exigem autenticação (`hasRole('USER')` ou superior).
- **Catálogo Administrativo (`POST`, `PUT`, `DELETE` em `/api/v1/products/**` e `/categories/**`)**: Restrito a `hasRole('ADMIN')`.

---

## 4. Tratamento Centralizado de Exceções (`ErrorHandler`)

- **`ApiException`**: Mapeia falhas funcionais (ex.: estoque insuficiente, pedido já enviado impedindo cancelamento) para o status HTTP e código configurados na exceção.
- **`MethodArgumentNotValidException`**: Formata erros de validação declarativa (`@NotBlank`, `@Min`, `@NotNull` no endereço de entrega) com status `400 Bad Request` e lista de campos afetados.
- **`DataIntegrityViolationException`**: Trata duplicidade de dados relacionais únicos (e-mail de usuário ou SKU de produto) retornando `409 Conflict`.

---

## 5. Observações Técnicas e Débitos Identificados
- **Tratamento Genérico de Falhas Inesperadas**: Adicionar manipulador `@ExceptionHandler(Exception.class)` com retorno `500 Internal Server Error` no formato de envelope padronizado.
- **Header H2 Console em Produção**: Restringir o cabeçalho `frameOptions(sameOrigin)` exclusivamente ao perfil de desenvolvimento local.
