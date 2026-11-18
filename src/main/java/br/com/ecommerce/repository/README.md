# Módulo: repository (`br.com.ecommerce.repository`)

## 1. Objetivo do Módulo
Fornecer a camada de persistência e acesso a dados para o banco de dados relacional H2, abstraindo operações CRUD e consultas derivadas através do Spring Data JPA no modelo **Single-Tenant**.

---

## 2. Responsabilidade Principal
- Declarar interfaces estendendo `JpaRepository<T, String>` para as entidades do sistema.
- Disponibilizar métodos de busca derivados para filtragem de produtos ativos, recuperação de histórico de pedidos e credenciais de autenticação.
- Isolar a camada de serviços de detalhes específicos de dialeto SQL ou comandos JDBC.

---

## 3. Repositórios e Métodos de Consulta

### 3.1. `CategoryRepository`
- Extensão de `JpaRepository<Category, String>`.
- Permite persistência da árvore de categorias com profundidade ilimitada.

### 3.2. `ProductRepository`
- Extensão de `JpaRepository<Product, String>`.
- **Consultas de Catálogo Ativo**:
  - `Page<Product> findByStatus(ProductStatus status, Pageable pageable);`
  - `Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String search, Pageable pageable);`
  - `Page<Product> findByStatusAndCategoriesId(ProductStatus status, String categoryId, Pageable pageable);`
  - Assegura que produtos inativos (`INACTIVE`) sejam filtrados diretamente na cláusula `WHERE` do banco de dados.

### 3.3. `PurchaseOrderRepository`
- Extensão de `JpaRepository<PurchaseOrder, String>`.
- `List<PurchaseOrder> findByUserId(String userId);`: Retorna todo o histórico de compras do usuário logado (listagem direta, sem paginação/filtros).
- Não há método de busca de pedidos por usuário para o perfil de administrador.

### 3.4. `PaymentRepository`
- Extensão de `JpaRepository<Payment, String>`.
- `Optional<Payment> findByOrderId(String orderId);`: Localiza a transação de pagamento atrelada ao pedido.

### 3.5. `UserRepository`
- Extensão de `JpaRepository<User, String>`.
- `Optional<User> findByEmail(String email);`: Utilizada para autenticação no login e validação de duplicidade cadastral.
- Sem filtros de multi-tenancy; unicidade de e-mail é global para a loja.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- `br.com.ecommerce.model.*`: Modelos JPA persistidos (`Category`, `Product`, `PurchaseOrder`, `Payment`, `User`, `DeliveryAddress`, `OrderStatus`, `ProductStatus`).

### Dependências Externas
- `org.springframework.data.jpa.repository.JpaRepository`: Interface raiz do Spring Data JPA.
- `org.springframework.data.domain.Page` e `Pageable`: Paginação nativa na fonte de dados.

---

## 5. Observações Técnicas e Débitos Identificados
- **Ativação dos Métodos de Paginação**: Migrar as consultas do catálogo para utilizarem diretamente os métodos derivados de `ProductRepository` com filtro `status = ACTIVE`, eliminando o processamento em memória de `CatalogService.products`.
- **Query de Bloqueio Concorrente**: Adicionar anotação `@Lock(LockModeType.PESSIMISTIC_WRITE)` em método específico de consulta de produto por ID para uso exclusivo durante a baixa de estoque no checkout.
