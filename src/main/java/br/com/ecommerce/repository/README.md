# Módulo: repository (`br.com.ecommerce.repository`)

## 1. Objetivo do Módulo
Fornecer a camada de persistência e acesso a dados para o banco de dados relacional H2, abstraindo as operações CRUD e consultas derivadas através do Spring Data JPA.

---

## 2. Responsabilidade Principal
- Declarar interfaces estendendo `JpaRepository<T, String>` para as entidades do sistema.
- Disponibilizar métodos de busca derivados (Derived Query Methods) baseados em convenções de nomenclatura do Spring Data JPA.
- Isolar a camada de serviços de detalhes específicos de dialeto SQL ou comandos JDBC.

---

## 3. Funcionalidades Existentes

### 3.1. `CategoryRepository`
- Extensão padrão de `JpaRepository<Category, String>`.
- Fornece operações básicas de persistência, exclusão e listagem de todas as categorias (`findAll()`).

### 3.2. `ProductRepository`
- Extensão de `JpaRepository<Product, String>`.
- Declaração de busca com paginação:
  `Page<Product> findByNameContainingIgnoreCase(String search, Pageable pageable);`

### 3.3. `PurchaseOrderRepository`
- Extensão de `JpaRepository<PurchaseOrder, String>`.
- Declaração de busca por usuário:
  `List<PurchaseOrder> findByUserId(String userId);`

### 3.4. `PaymentRepository`
- Extensão de `JpaRepository<Payment, String>`.
- Declaração de busca por pedido:
  `Optional<Payment> findByOrderId(String orderId);`

### 3.5. `UserRepository`
- Extensão de `JpaRepository<User, String>`.
- Declaração de busca por e-mail (usada para login e validação de duplicidade):
  `Optional<User> findByEmail(String email);`

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.model.*`: Mapeamento das entidades persistidas (`Category`, `Product`, `PurchaseOrder`, `Payment`, `User`).

### Dependências Externas
- `org.springframework.data.jpa.repository.JpaRepository`: Interface raiz do Spring Data JPA.
- `org.springframework.data.domain.Page`: Encapsulamento de resultados paginados.
- `org.springframework.data.domain.Pageable`: Parâmetro de especificação de página, tamanho e ordenação.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.model`: Fornece os modelos relacionais que parametrizam cada repositório.
- `br.com.ecommerce.service`: Consome diretamente as operações disponibilizadas pelos repositórios.

---

## 6. Pontos de Entrada
Os métodos padrão herdados de `JpaRepository` (`save`, `saveAndFlush`, `findById`, `findAll`, `delete`) e as consultas derivadas declaradas nas interfaces.

---

## 7. Fluxos Importantes
- **Busca por E-mail no Login**: `UserRepository.findByEmail` gera a consulta SQL `SELECT ... FROM users WHERE email = ?` utilizada para validar unicidade e verificar hashes de senha.
- **Busca de Pedidos por Usuário**: `PurchaseOrderRepository.findByUserId` gera `SELECT ... FROM orders WHERE user_id = ?` para o histórico de compras do cliente.
- **Busca de Pagamento por Pedido**: `PaymentRepository.findByOrderId` recupera o pagamento vinculado à chave estrangeira `order_id`.

---

## 8. Arquivos Críticos
- `UserRepository.java`: Ponto de validação de identidades para segurança.
- `PurchaseOrderRepository.java`: Persistência de transações comerciais.
- `ProductRepository.java`: Catálogo de produtos e controle de estoque.

---

## 9. Observações Técnicas e Débitos Identificados
- **Método Órfão Não Utilizado**: O método `findByNameContainingIgnoreCase(String search, Pageable pageable)` em `ProductRepository` é código morto na implementação atual, pois `CatalogService` opta por carregar todos os produtos em memória com `findAll()` e filtrar via Java Streams.
- **Falta de Métodos de Consulta Filtrados no Banco**: Não há métodos declarados ou consultas `@Query` para filtrar produtos por categoria no nível do banco de dados, agravando o problema de carregamento integral da tabela na camada de serviço.
- **Ausência de Locking Queries**: Não há declarações com `@Lock(LockModeType.PESSIMISTIC_WRITE)` para a recuperação de produtos durante a operação de checkout, o que expõe o sistema a condições de corrida em concorrência real.
