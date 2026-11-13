# Módulo: config (`br.com.ecommerce.config`)

## 1. Objetivo do Módulo
Configurar os componentes transversais de infraestrutura da aplicação, especificamente o pipeline de segurança HTTP, autenticação stateless baseada em JSON Web Tokens (OAuth2 Resource Server) e o tratamento global e centralizado de exceções na camada REST.

---

## 2. Responsabilidade Principal
- Definir regras de controle de acesso a rotas públicas e privadas via Spring Security (`SecurityFilterChain`).
- Configurar decodificadores e codificadores JWT com criptografia simétrica HMAC-SHA256 (`NimbusJwtDecoder` e `NimbusJwtEncoder`).
- Mapear a claim customizada `role` do token para a autoridade `ROLE_<ROLE>` reconhecida pelo Spring Security.
- Capturar exceções de negócio (`ApiException`), falhas de validação de payload (`MethodArgumentNotValidException`) e erros de integridade relacional (`DataIntegrityViolationException`), transformando-os em respostas HTTP padronizadas.

---

## 3. Funcionalidades Existentes
- **BCrypt Password Encoder**: Bean `PasswordEncoder` instanciando `BCryptPasswordEncoder` para hash seguro de senhas de usuários.
- **Assinatura e Validação Simétrica de JWT**: Beans `SecretKey`, `JwtDecoder` e `JwtEncoder` alimentados pela propriedade `app.jwt-secret`.
- **Filtro de Segurança HTTP**:
  - Desativação de CSRF (`AbstractHttpConfigurer::disable`) adequada para APIs REST stateless.
  - Liberação de frames do mesmo domínio (`headers(h -> h.frameOptions(f -> f.sameOrigin()))`) para permitir visualização da interface web do console H2.
  - Política de criação de sessão definida como `STATELESS`.
  - Liberação de rotas públicas: `/api/v1/auth/**`, `GET /api/v1/products/**`, `GET /api/v1/categories/**`, `/actuator/health`, `/h2-console/**`.
  - Exigência de autenticação para todas as demais rotas.
- **Tratamento de Exceções REST (`ErrorHandler`)**:
  - `ApiException`: Converte o status e mensagem da exceção em payload `{"error": {"message": ..., "code": ...}}`.
  - `MethodArgumentNotValidException`: Captura erros de Bean Validation (`@Valid`) e retorna `400 Bad Request` com lista detalhada `{"error": {"message": "Dados inválidos", "details": [...]}}`.
  - `DataIntegrityViolationException`: Intercepta violações de constraints do banco (ex: e-mail ou SKU duplicado) e retorna `409 Conflict`.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.service.ApiException`: Utilizada pelo `ErrorHandler` para interceptar e extrair o código de status HTTP correspondente.

### Dependências Externas
- `org.springframework.security:spring-security-config`: Configurações de segurança web e filtros.
- `org.springframework.security:spring-security-oauth2-resource-server`: Suporte a Resource Server e resolução de tokens Bearer.
- `org.springframework.security:spring-security-oauth2-jose`: Codificadores e decodificadores Nimbus JOSE/JWT.
- `org.springframework.security.crypto.bcrypt`: Algoritmo de hash BCrypt.
- `org.springframework.boot:spring-boot-starter-web`: Anotações `@RestControllerAdvice`, `@ExceptionHandler`, `ResponseEntity`.
- `org.springframework.dao:spring-tx / spring-jdbc`: Classe `DataIntegrityViolationException`.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.controller`: Todos os controladores dependem dos filtros de segurança configurados neste módulo para validação de token e autorização, além de delegarem erros não capturados para o `ErrorHandler`.
- `br.com.ecommerce.service`: `AuthService` injeta `PasswordEncoder` e `JwtEncoder` expostos pelo `SecurityConfig`. Serviços lançam `ApiException` tratada por este módulo.

---

## 6. Pontos de Entrada
- `SecurityFilterChain filterChain(HttpSecurity http)`: Interceptador de todas as requisições HTTP entrantes no servidor Tomcat.
- Métodos do `@RestControllerAdvice` em `ErrorHandler`: Disparados automaticamente pelo Spring Web quando exceções são propagadas fora dos controladores.

---

## 7. Fluxos Importantes
1. **Fluxo de Interceptação de Requisição Autenticada**:
   - Requisição chega com `Authorization: Bearer <token>`.
   - O filtro do Spring Security aciona `jwtDecoder` configurado em `SecurityConfig`.
   - O `jwtAuthConverter` extrai a claim `role` e adiciona `ROLE_USER` ou `ROLE_ADMIN` ao contexto de segurança do Spring.
   - A requisição é liberada para os controladores ou barrada com `401 Unauthorized`.
2. **Fluxo de Captura de Falha de Validação**:
   - Cliente envia JSON inválido para um `@Valid @RequestBody`.
   - Spring dispara `MethodArgumentNotValidException`.
   - `ErrorHandler.validation` captura a exceção, itera sobre os erros de campo e formata resposta `400 Bad Request`.

---

## 8. Arquivos Críticos
- `SecurityConfig.java`: Arquivo central de controle de segurança, beans criptográficos e autorização de endpoints.
- `ErrorHandler.java`: Manipulador centralizado que assegura que nenhum stack trace interno vaze para clientes da API.

---

## 9. Observações Técnicas e Débitos Identificados
- **Criptografia Simétrica**: O uso de HMAC-SHA256 (`MacAlgorithm.HS256`) significa que o mesmo segredo é usado para assinar e validar tokens. [Hipótese] Para arquiteturas distribuídas com microsserviços, recomenda-se migrar para criptografia assimétrica (RSA/ECDSA com par de chaves pública/privada).
- **Tratamento Genérico de Erros Inesperados**: O `ErrorHandler` não possui um manipulador para `Exception.class` genérica (`500 Internal Server Error`). Exceções não previstas (ex: `NullPointerException`) resultarão na resposta padrão do Spring Boot em vez do envelope `{"error": ...}`.
- **Header H2 Console em Produção**: `frameOptions(f -> f.sameOrigin())` foi habilitado especificamente para permitir frames do console H2. Deve ser desativado ou restrito caso o console H2 seja desligado em produção.
