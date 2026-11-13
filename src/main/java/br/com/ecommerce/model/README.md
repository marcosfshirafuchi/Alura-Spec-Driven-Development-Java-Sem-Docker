# Módulo: model (`br.com.ecommerce.model`)

## 1. Objetivo do Módulo
Representar as entidades do modelo de domínio do comércio eletrônico e definir os mapeamentos objeto-relacional (ORM) para persistência no banco de dados relacional H2, além de disponibilizar as enumerações que definem os estados e papéis do sistema.

---

## 2. Responsabilidade Principal
- Declarar o esquema de tabelas relacionais (`@Table`, `@Column`, `@JoinColumn`, `@Index`).
- Estabelecer os relacionamentos de chave estrangeira entre entidades (`@ManyToOne`, `@OneToMany`, `@OneToOne`).
- Configurar comportamento de cascata e ciclo de vida de agregação (ex: `orphanRemoval = true` entre pedido e seus itens).
- Definir constantes de estado e autorização via enums tipados.

---

## 3. Funcionalidades Existentes

### 3.1. Entidades JPA
1. **`User` (Tabela: `users`)**:
   - `id`: String (UUIDv4).
   - `name`: Nome do usuário.
   - `email`: E-mail único e obrigatório (`@Column(unique = true, nullable = false)`).
   - `password`: Hash da senha (`@JsonIgnore` para evitar vazamento em serializações).
   - `role`: Enum `Role` (`USER`, `ADMIN`), mapeado como `EnumType.STRING`.
   - `tenantId`: Identificador de inquilino (padrão: `"default"`).
   - `createdAt`, `updatedAt`: Timestamps UTC (`Instant.now()`).

2. **`Category` (Tabela: `categories`)**:
   - `id`: String (UUIDv4).
   - `name`: Nome da categoria.
   - `parent`: Auto-relacionamento `@ManyToOne(fetch = FetchType.LAZY)` mapeando categoria pai opcional com `@JsonIgnore`.
   - `createdAt`, `updatedAt`: Timestamps UTC.

3. **`Product` (Tabela: `products`)**:
   - Índice definido no banco para o campo `name` (`@Table(indexes = @Index(columnList = "name"))`).
   - `id`: String (UUIDv4).
   - `name`: Nome do produto.
   - `description`: Descrição textual de até 4000 caracteres (`@Column(length = 4000)`).
   - `sku`: Código SKU único e obrigatório (`@Column(unique = true, nullable = false)`).
   - `price`: Valor unitário (`@Column(precision = 10, scale = 2)`).
   - `stock`: Saldo de estoque físico em inteiros (padrão: `0`).
   - `status`: Enum `ProductStatus` (`ACTIVE`, `INACTIVE`), mapeado como String.
   - `tenantId`: Identificador de inquilino (padrão: `"default"`).
   - `category`: Chave estrangeira obrigatória `@ManyToOne(optional = false)` para `Category`.
   - `createdAt`, `updatedAt`: Timestamps UTC.

4. **`PurchaseOrder` (Tabela: `orders`)**:
   - `id`: String (UUIDv4).
   - `user`: Chave estrangeira `@ManyToOne(optional = false)` para `User`.
   - `status`: Enum `OrderStatus` (`PENDING`, `PAID`, `CANCELED`), padrão `PENDING`.
   - `totalValue`: Valor total monetário calculado (`BigDecimal`, precisão 10, escala 2).
   - `tenantId`: Identificador de inquilino (padrão: `"default"`).
   - `items`: Lista de `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` vinculando `OrderItem`.
   - `createdAt`, `updatedAt`: Timestamps UTC.

5. **`OrderItem` (Tabela: `order_items`)**:
   - `id`: String (UUIDv4).
   - `order`: Chave estrangeira lazy para o pedido pai (`@JsonIgnore`).
   - `product`: Chave estrangeira obrigatória `@ManyToOne(optional = false)` para `Product`.
   - `quantity`: Quantidade do item.
   - `price`: Snapshot do preço unitário cobrado no momento da compra (`BigDecimal(10, 2)`).

6. **`Payment` (Tabela: `payments`)**:
   - `id`: String (UUIDv4).
   - `order`: Relacionamento 1 para 1 exclusivo `@OneToOne(optional = false)` com `@JoinColumn(name = "order_id", unique = true)`.
   - `status`: Estado do pagamento em String (padrão: `"AWAITING_CONFIRMATION"`).
   - `externalId`: Identificador externo de transação em gateway financeiro.
   - `createdAt`, `updatedAt`: Timestamps UTC.

### 3.2. Enumerações
- **`Role`**: Perfis de usuário no sistema: `USER` e `ADMIN`.
- **`ProductStatus`**: Estados de produto: `ACTIVE` e `INACTIVE`.
- **`OrderStatus`**: Estados do ciclo do pedido: `PENDING`, `PAID`, `CANCELED`.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- Relações entre as próprias classes do pacote `model` (`PurchaseOrder` $\rightarrow$ `OrderItem`, `Product` $\rightarrow$ `Category`, etc.).

### Dependências Externas
- `jakarta.persistence.*`: Anotações JPA standard para mapeamento de entidades.
- `com.fasterxml.jackson.annotation.JsonIgnore`: Supressão de campos sensíveis ou que possam causar loops de recursão em serialização JSON.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.repository`: Utiliza essas entidades como tipo genérico base de persistência.
- `br.com.ecommerce.service`: Instancia e altera o estado das entidades durante os fluxos de negócio.

---

## 6. Pontos de Entrada
Instanciação nos serviços (`new Product()`, `new PurchaseOrder()`, etc.) ou recuperação via consultas do Spring Data JPA.

---

## 7. Fluxos Importantes
- **Ciclo de Vida de Pedido e Itens**: Como `PurchaseOrder.items` possui `cascade = CascadeType.ALL` e `orphanRemoval = true`, ao persistir ou remover um pedido, todos os seus respectivos itens são automaticamente persistidos ou removidos em cascata.
- **Isolamento de Categoria Pai e Pedido Pai**: O uso de `@JsonIgnore` em `Category.parent` e `OrderItem.order` previne serialização infinita em ciclos bidirecionais quando os objetos são retornados para o Jackson.

---

## 8. Arquivos Críticos
- `Product.java`: Entidade central do catálogo e ponto focal de controle de estoque e concorrência.
- `PurchaseOrder.java`: Entidade raiz do agregado de vendas.
- `Payment.java`: Entidade de conciliação financeira do pedido.

---

## 9. Observações Técnicas e Débitos Identificados
- **Ausência de Encapsulamento (Campos Públicos)**: Todos os atributos em todas as classes de modelo são declarados como `public` diretamente, sem métodos `getters` e `setters`, nem métodos de domínio com proteção de invariantes (ex: `product.stock = -5` pode ser executado diretamente sem impedimento pela entidade).
- **Inconsistência de Tipos de Status no Pagamento**: Enquanto `OrderStatus` e `ProductStatus` usam enums dedicados, `Payment.status` é uma `String` pura manipulada com strings mágicas (`"AWAITING_CONFIRMATION"`, `"PAID"`, `"CANCELED"`). Recomenda-se a criação do enum `PaymentStatus`.
- **Campo Multi-tenancy Órfão**: `tenantId` está presente em `User`, `Product` e `PurchaseOrder`, mas não há anotação de filtro (`@FilterDef`, `@Filter` do Hibernate) ou cláusula `WHERE tenant_id = ...` nas consultas.
- **Falta de Controle de Versão Otimista**: Não há campo anotado com `@Version` em `Product`, o que permitiria ao JPA evitar conflitos de atualização concorrente no estoque de forma automática.
