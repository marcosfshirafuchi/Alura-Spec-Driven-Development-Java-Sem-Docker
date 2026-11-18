# Módulo: model (`br.com.ecommerce.model`)

## 1. Objetivo do Módulo
Representar as entidades do modelo de domínio do comércio eletrônico e definir os mapeamentos objeto-relacional (ORM) para persistência no banco de dados relacional H2, além de disponibilizar as enumerações que definem os estados e papéis do sistema em arquitetura **Single-Tenant (loja única)**.

---

## 2. Responsabilidade Principal
- Declarar o esquema de tabelas relacionais (`@Table`, `@Column`, `@JoinColumn`, `@Index`, `@ManyToMany`, `@JoinTable`).
- Estabelecer os relacionamentos de chave estrangeira entre entidades (`@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`).
- Configurar comportamento de cascata e ciclo de vida de agregação (composição de `PurchaseOrder` com `OrderItem` e `DeliveryAddress`).
- Definir constantes de estado e autorização via enums tipados (`OrderStatus`, `ProductStatus`, `Role`).

---

## 3. Funcionalidades Existentes e Regras do Domínio

### 3.1. Entidades JPA

1. **`User` (Tabela: `users`)**:
   - `id`: String (UUIDv4).
   - `name`: Nome do usuário.
   - `email`: E-mail único e obrigatório (`@Column(unique = true, nullable = false)`).
   - `password`: Hash da senha (`@JsonIgnore`).
   - `role`: Enum `Role` (`USER`, `ADMIN`), mapeado como `EnumType.STRING`.
   - `createdAt`, `updatedAt`: Timestamps UTC (`Instant.now()`).
   - **Regra de Governança**: Usuários comuns não possuem permissão de exclusão da própria conta (`DELETE /users/{id}` é restrito a `ADMIN`). Usuários logados podem alterar sua própria senha.
   - *Nota de Arquitetura*: O campo legado `tenantId` foi formalmente eliminado do modelo.

2. **`Category` (Tabela: `categories`)**:
   - `id`: String (UUIDv4).
   - `name`: Nome da categoria.
   - `parent`: Auto-relacionamento `@ManyToOne(fetch = FetchType.LAZY)` permitindo árvore hierárquica com **profundidade ilimitada**.
   - `createdAt`, `updatedAt`: Timestamps UTC.

3. **`Product` (Tabela: `products`)**:
   - `id`: String (UUIDv4).
   - `name`: Nome do produto.
   - `description`: Descrição textual de até 4000 caracteres.
   - `sku`: Código SKU único e obrigatório.
   - `price`: Valor unitário (`BigDecimal(10, 2)`).
   - `stock`: Saldo de estoque físico em inteiros.
   - `status`: Enum `ProductStatus` (`ACTIVE`, `INACTIVE`).
   - `categories`: Relacionamento de muitos-para-muitos (`@ManyToMany`) com tabela associativa `product_categories`. **Um produto pode pertencer a mais de uma categoria**.
   - `createdAt`, `updatedAt`: Timestamps UTC.
   - **Regra de Visibilidade**: Quando um produto é alterado para `INACTIVE`, ele sai imediatamente do catálogo público de vendas.

4. **`DeliveryAddress` (Tabela / Embeddable: `delivery_addresses` ou embutido em `orders`)**:
   - Composição de endereço do pedido: `street`, `number`, `complement`, `neighborhood`, `city`, `state`, `zipCode`.
   - Armazenado como snapshot imutável no momento da criação do pedido.

5. **`PurchaseOrder` (Tabela: `orders`)**:
   - `id`: String (UUIDv4).
   - `user`: Chave estrangeira `@ManyToOne(optional = false)` para `User`.
   - `status`: Enum `OrderStatus` (`PENDING`, `PAID`, `PACKING`, `SHIPPED`, `DELIVERED`, `CANCELED`).
   - `totalValue`: Valor monetário total (`BigDecimal(10, 2)`).
   - `deliveryAddress`: Endereço de entrega composto na ordem.
   - `items`: Lista de `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` vinculando `OrderItem`.
   - `createdAt`, `updatedAt`: Timestamps UTC.

6. **`OrderItem` (Tabela: `order_items`)**:
   - `id`: String (UUIDv4).
   - `order`: Chave estrangeira lazy para o pedido pai.
   - `product`: Chave estrangeira obrigatória `@ManyToOne(optional = false)` para `Product`.
   - `quantity`: Quantidade do item.
   - `price`: Snapshot do preço unitário cobrado no momento do checkout.
   - **Regra de Negócio**: Não há suporte a cancelamento parcial de itens.

7. **`Payment` (Tabela: `payments`)**:
   - `id`: String (UUIDv4).
   - `order`: Relacionamento `@OneToOne(optional = false)` com `@JoinColumn(name = "order_id", unique = true)`.
   - `status`: Estado do pagamento (`AWAITING_CONFIRMATION`, `PAID`, `CANCELED`).
   - `externalId`: Identificador externo de transação.
   - `createdAt`, `updatedAt`: Timestamps UTC.

---

## 4. Enumerações

### 4.1. `Role`
- `USER`: Cliente comum da loja.
- `ADMIN`: Administrador com poderes de gestão de catálogo, usuários e despacho logístico.

### 4.2. `ProductStatus`
- `ACTIVE`: Visível no catálogo público e disponível para compra.
- `INACTIVE`: Desativado; removido imediatamente da visibilidade do catálogo público.

### 4.3. `OrderStatus`
| Enum Value | Descrição Operacional | Quem altera para este status? |
|---|---|---|
| `PENDING` | Pedido gerado no checkout, aguardando liquidação | Sistema |
| `PAID` | Pagamento confirmado via chamada do front-end | Front-end / Sistema |
| `PACKING` | Pedido em processo de separação física no estoque | Apenas `ADMIN` |
| `SHIPPED` | Pedido despachado para a transportadora | Apenas `ADMIN` |
| `DELIVERED` | Pedido entregue ao cliente | `USER` (marcar recebido) ou `ADMIN` |
| `CANCELED` | Pedido cancelado integralmente | `USER` ou `ADMIN` (se elegível) |

---

## 5. Regras Críticas de Integridade do Modelo

1. **Restrição de Estorno de Estoque por Status**:
   - Cancelamento com devolução de estoque só é permitido se `status` for `PENDING`, `PAID` ou `PACKING`.
   - Pedidos com status `SHIPPED` ou `DELIVERED` bloqueiam cancelamento com estorno direto de estoque.
2. **Proibição de Cancelamento Parcial**:
   - Não é permitida a exclusão ou devolução de `OrderItem` isolado de um pedido existente. O cancelamento é sempre total sobre a raiz `PurchaseOrder`.
3. **Descarte de Multi-Tenancy**:
   - A coluna e atributo `tenantId` foi descartada; o banco opera com visão unificada de loja única.

---

## 6. Observações Técnicas e Débitos Identificados

- **Tipagem de Status de Pagamento**: Criar o enum dedicado `PaymentStatus` para eliminar manipulação de strings mágicas em `Payment.status`.
- **Encapsulamento de Atributos**: Mudar modificadores de campo de `public` para `private` com métodos de acesso e métodos de domínio expressivos (ex: `product.deductStock(qty)`, `order.markPacking()`).
- **Controle Otimista de Concorrência**: Adicionar anotação `@Version private Long version;` na entidade `Product` para proteção automática contra *overselling*.
