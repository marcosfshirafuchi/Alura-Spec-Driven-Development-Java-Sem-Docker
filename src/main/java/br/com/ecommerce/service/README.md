# Módulo: service (`br.com.ecommerce.service`)

## 1. Objetivo do Módulo
Concentrar toda a lógica de negócio, regras de domínio, validações funcionais, transações atômicas de banco de dados, emissão de tokens criptográficos e gerenciamento de estado de carrinhos de compras da aplicação e-commerce.

---

## 2. Responsabilidade Principal
- Implementar as invariantes e regras operacionais do sistema de comércio eletrônico.
- Orquestrar operações entre múltiplos repositórios e serviços auxiliares.
- Garantir a integridade das operações de checkout, decremento e reposição de estoque com `@Transactional`.
- Gerenciar geração de tokens JWT (acesso e refresh) e verificação de senhas com algoritmo BCrypt.
- Manter o armazenamento de dados transitórios de carrinho de compras em memória.
- Fornecer exceções customizadas de domínio (`ApiException`) com semântica de status HTTP.

---

## 3. Funcionalidades Existentes

### 3.1. `AuthService`
- Validação de unicidade de e-mail no cadastro.
- Hash de senhas com `PasswordEncoder`.
- Autenticação e verificação de credenciais em login.
- Construção e assinatura de tokens JWT:
  - `accessToken`: Validade de 900 segundos (15 minutos), contendo claims `role`, `type: "access"`, `sub: userId`, `iss: "ecommerce-springboot"`.
  - `refreshToken`: Validade de 604.800 segundos (7 dias), contendo claim `type: "refresh"`.

### 3.2. `CartService`
- Gerenciamento de carrinho em memória através de `ConcurrentHashMap<String, String>` com chaves prefixadas `cart:default:{userId}`.
- Serialização e deserialização do estado do carrinho com Jackson `ObjectMapper`.
- Adição de item validando existência, status `ProductStatus.ACTIVE` e saldo de estoque suficiente (`stock >= existing + quantity`).
- Remoção individual de item do carrinho por ID do produto.
- Limpeza integral do carrinho após checkout (`clear(userId)`).

### 3.3. `CatalogService`
- CRUD completo de categorias (`CategoryInput`) com relacionamento hierárquico opcional (`parentId`).
- CRUD de produtos (`ProductInput`) validando campos obrigatórios, preço não negativo e categoria vinculada.
- Listagem de produtos com filtros em memória: busca textual por substring (`search`), categoria (`categoryId`) e cálculo manual de paginação (`page`, `limit`).

### 3.4. `OrderService`
- Fechamento de pedido transacional (`@Transactional checkout`):
  - Verifica se o carrinho do usuário não está vazio.
  - Revalida status ativo e estoque disponível de cada produto no banco.
  - Decrementa o estoque de cada produto em tempo real (`p.stock -= item.quantity`).
  - Cria e persiste `PurchaseOrder` com status `OrderStatus.PENDING` e valor total calculado.
  - Cria instâncias de `OrderItem` contendo snapshot do preço e quantidade do momento da compra.
  - Limpa o carrinho do usuário após a gravação atômica (`saveAndFlush`).
- Listagem de histórico de pedidos do usuário logado (`list(userId)`).
- Consulta de pedido por ID (`find(id)`).

### 3.5. `PaymentService`
- Criação ou recuperação de pagamento pendente associado a um pedido (`create(orderId, userId)`). Valida titularidade do pedido.
- Confirmação de pagamento (`@Transactional confirm`):
  - Exige a presença do cabeçalho `Idempotency-Key`.
  - Transiciona o pagamento para `PAID` e o pedido para `OrderStatus.PAID`.
- Cancelamento de pagamento (`@Transactional cancel`):
  - Verifica se o pagamento já não foi pago.
  - Transiciona o pagamento para `CANCELED` e o pedido para `OrderStatus.CANCELED`.
  - Restaura as quantidades dos itens do pedido ao estoque dos produtos correspondentes (`item.product.stock += item.quantity`).

### 3.6. `UserService`
- Consulta, atualização de perfil e exclusão de contas.
- Controle de autorização (`check`): Garante que apenas o próprio titular ou um usuário com papel `ADMIN` possam consultar, alterar ou excluir um cadastro.
- Re-codificação segura de senha ao atualizar via `passwordEncoder.encode(...)`.

### 3.7. `ApiException`
- Subclasse de `RuntimeException` contendo atributo `public final HttpStatus status`, permitindo que qualquer serviço interrompa o fluxo com status HTTP apropriado.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.model.*`: Manipulação direta de todas as entidades de domínio (`User`, `Category`, `Product`, `PurchaseOrder`, `OrderItem`, `Payment`, enums).
- `br.com.ecommerce.repository.*`: Injeção de todos os repositórios JPA.
- Inter-dependência de Serviços:
  - `OrderService` injeta `CartService`.
  - `PaymentService` injeta `OrderService` e `PurchaseOrderRepository`.

### Dependências Externas
- `org.springframework.stereotype.Service`: Registro de componentes no contexto Spring.
- `org.springframework.transaction.annotation.Transactional`: Gerenciamento transacional de banco de dados.
- `org.springframework.security.crypto.password.PasswordEncoder`: Hash BCrypt.
- `org.springframework.security.oauth2.jwt.*`: Criação de claims e codificação de JWT com `JwtEncoder`.
- `com.fasterxml.jackson.databind.ObjectMapper`: Serialização de registros em JSON no `CartService`.
- `jakarta.validation.constraints.*`: Regras de validação em records de entrada.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.controller`: Consome diretamente os métodos e records públicos deste módulo.
- `br.com.ecommerce.repository`: Fornece a camada de persistência consumida pelos serviços.
- `br.com.ecommerce.config`: Utiliza `ApiException` no `ErrorHandler` e expõe beans de infraestrutura consumidos por `AuthService`.

---

## 6. Pontos de Entrada
Os métodos públicos dos 6 serviços constituem os pontos de entrada das regras de negócio invocados exclusivamente pelos controladores REST.

---

## 7. Fluxos Importantes
- **Fluxo Atômico de Checkout**:
  1. Leitura de carrinho em memória.
  2. Validação sequencial de itens e estoque.
  3. Atualização de estoque no repositório de produtos.
  4. Persistência de pedido e itens no banco.
  5. Descarte de carrinho em memória.
  6. Caso qualquer falha ocorra (ex: estoque insuficiente no meio da lista), o Spring executa rollback da transação.

---

## 8. Arquivos Críticos
- `OrderService.java`: Contém o coração transacional da aplicação (transformação de intenção em venda efetiva e controle de estoque).
- `PaymentService.java`: Orquestra o ciclo financeiro e estorno de estoque em cancelamentos.
- `CartService.java`: Gerencia a integridade do carrinho de compras em memória.
- `AuthService.java`: Responsável pela segurança criptográfica e identidade dos usuários.

---

## 9. Observações Técnicas e Débitos Identificados
- **Filtragem e Paginação de Catálogo em Memória**: `CatalogService.products` executa `products.findAll()` e faz o filtro através de Streams na JVM. Deve ser migrado urgentemente para consultas paginadas nativas via `ProductRepository.findAll(Pageable)`.
- **Ausência de Lock Concorrente no Estoque**: `OrderService.checkout` verifica e debita estoque sem bloqueio (`PESSIMISTIC_WRITE` ou `@Version`), possibilitando sobre-venda (*overselling*) sob requisições simultâneas.
- **Volatilidade de Dados do Carrinho**: O `ConcurrentHashMap` de `CartService` perde dados em reinicializações do servidor. Recomenda-se migrar para Redis com chave TTL.
- **Acoplamento Cruzado de Repositório**: `PaymentService` injeta `PurchaseOrderRepository` diretamente para salvar o pedido (`orderRepo.save(p.order)`), violando o isolamento entre serviços de domínio distintos.
- **Simulação Frágil de Idempotência**: `PaymentService.confirm` valida se a chave `Idempotency-Key` foi informada, mas não armazena a chave nem valida se a mesma requisição já foi processada anteriormente.
- **Endpoint Faltante para Refresh Token**: `AuthService` gera `refreshToken`, porém não existe nenhum método ou endpoint implementado para renovar o acesso utilizando esse token.
- **Estorno de Estoque em Cancelamento**: Em `PaymentService.cancel`, o loop atualiza `item.product.stock += item.quantity` e executa `orderRepo.save(p.order)`. Como a relação entre `OrderItem` e `Product` não possui cascade de atualização, a persistência do novo estoque depende estritamente do dirty-checking da sessão Hibernate ativa. Recomenda-se injeção explícita de `ProductRepository.save(product)`.
