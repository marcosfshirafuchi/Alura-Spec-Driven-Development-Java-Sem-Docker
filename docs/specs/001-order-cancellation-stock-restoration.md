# Feature Specification: Cancelamento de Pedidos com Estorno Condicional de Estoque

**Feature Branch**: `001-order-cancellation-stock-restoration`

**Created**: 2026-11-19

**Status**: Draft

**Input**: User description: "Sobre o cancelamento e estoque só pode ser restaurado se o pedido não foi marcado como enviado e ainda estava em separação ou aguardando pagamento. Não fazemos cancelamento parcial dos itens, isso deve ser proibido."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cancelamento Total com Estorno de Estoque em Fases Pré-Envio (Priority: P1)

Como cliente comprador ou administrador da loja, desejo cancelar integralmente um pedido que ainda esteja aguardando pagamento, pago ou em separação, para que a compra seja cancelada e todos os itens retornem imediatamente ao estoque disponível para outros clientes.

**Why this priority**: É a regra central de negócio e a fatia mínima viável (MVP). Garante que pedidos desistidos liberem o estoque físico retido imediatamente, evitando perdas comerciais e divergências de inventário.

**Independent Test**: Pode ser testado de forma totalmente independente criando um pedido com 1 ou mais itens (que debita estoque), acionando o cancelamento enquanto o pedido está em `PENDING`, `PAID` ou `PACKING`, e verificando que o status muda para `CANCELED` e o estoque de cada produto é exatamente restabelecido.

**Acceptance Scenarios**:

1. **Given** um pedido com status `PENDING` contendo 2 unidades do Produto X (estoque atual = 8, estoque anterior = 10), **When** o cliente requisita o cancelamento total do pedido, **Then** o status do pedido é alterado para `CANCELED` e o estoque do Produto X retorna para 10 unidades.
2. **Given** um pedido com status `PACKING` contendo múltiplos produtos distintos, **When** o administrador ou cliente requisita o cancelamento, **Then** o pedido é marcado como `CANCELED` e as quantidades de todos os itens são devolvidas atomicamente aos respectivos estoques.

---

### User Story 2 - Bloqueio de Cancelamento com Estorno para Pedidos Já Despachados (Priority: P2)

Como administrador e operador de inventário, quero que o sistema impeça o cancelamento automático de pedidos que já foram despachados (`SHIPPED`) ou entregues (`DELIVERED`), para evitar a criação de estoque fantasma de produtos que já saíram do controle físico da loja.

**Why this priority**: Protege a integridade física e fiscal da loja contra fraude e inconsistência de inventário após o produto entrar em trânsito com a transportadora.

**Independent Test**: Pode ser testado de forma independente marcando um pedido como `SHIPPED`, disparando a requisição de cancelamento e verificando que a API recusa com código `400 Bad Request`, mantendo o status `SHIPPED` e o saldo de estoque inalterado.

**Acceptance Scenarios**:

1. **Given** um pedido com status `SHIPPED`, **When** o cliente ou administrador tenta cancelar o pedido via endpoint de cancelamento, **Then** o sistema rejeita a operação com erro HTTP `400 Bad Request` informando que pedidos já enviados não podem ser cancelados diretamente, e o estoque não sofre alteração.
2. **Given** um pedido com status `DELIVERED`, **When** uma requisição de cancelamento é enviada, **Then** o sistema rejeita a operação com erro HTTP `400 Bad Request` e nenhum estoque é estornado.

---

### User Story 3 - Proibição de Cancelamento Parcial de Itens (Priority: P3)

Como gestor financeiro e de estoque, quero garantir que cancelamentos operem exclusivamente sobre a totalidade do pedido, impedindo que itens isolados sejam cancelados ou removidos de pedidos fechados.

**Why this priority**: Elimina complexidade acidental de recálculo de frete, split de pagamentos, estornos parciais em gateway e divergência de notas fiscais.

**Independent Test**: Pode ser testado isoladamente garantindo a ausência ou bloqueio estrito de endpoints ou parâmetros que permitam deletar/cancelar um `OrderItem` específico.

**Acceptance Scenarios**:

1. **Given** um pedido ativo com 3 itens diferentes, **When** qualquer cliente ou operador tenta solicitar o cancelamento de apenas 1 dos itens, **Then** a requisição é terminantemente rejeitada, exigindo o cancelamento do pedido completo ou mantendo o pedido inalterado.

---

### Edge Cases

- **Tentativa de cancelamento duplicado (Idempotência)**: O que acontece quando duas requisições de cancelamento para o mesmo pedido chegam quase simultaneamente? O sistema deve garantir que o estorno de estoque ocorra exatamente uma vez, retornando o pedido já cancelado sem duplicar reposição de estoque.
- **Cancelamento durante transição de status**: O que acontece se o cliente dispara o cancelamento no exato milissegundo em que o operador logístico marca o pedido como `SHIPPED`? O bloqueio transacional deve avaliar o status consolidado: se a transição para `SHIPPED` comitar primeiro, o cancelamento subsequente deve ser barrado com `400`.
- **Cancelamento de produto inativo (`INACTIVE`)**: Se um produto foi inativado no catálogo após o pedido ter sido feito, ao cancelar o pedido o seu estoque numérico ainda deve ser recomposto normalmente, sem reativar automaticamente o produto no catálogo.
- **Tentativa de cancelamento por terceiro**: Usuário comum tentando cancelar pedido pertencente a outro cliente deve receber erro `403 Forbidden`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE permitir o cancelamento integral de pedidos que estejam exclusivamente nos estados `PENDING`, `PAID` ou `PACKING`.
- **FR-002**: O sistema DEVE restaurar automaticamente as quantidades exatas de todos os itens do pedido ao estoque dos respectivos produtos ao confirmar o cancelamento, dentro de uma única transação atômica.
- **FR-003**: O sistema DEVE rejeitar com status HTTP `400 Bad Request` qualquer tentativa de cancelamento de pedidos cujo status seja `SHIPPED` ou `DELIVERED`.
- **FR-004**: O sistema NÃO DEVE permitir cancelamento parcial de itens de um pedido; a operação de cancelamento é estritamente atômica sobre todo o agregado do pedido.
- **FR-005**: O sistema DEVE autorizar o cancelamento apenas se o requisitante for o próprio titular do pedido (`ROLE_USER`) ou um administrador (`ROLE_ADMIN`).
- **FR-006**: Caso um pagamento associado ao pedido seja cancelado (`POST /api/v1/payments/{id}/cancel`), o sistema DEVE sincronizar o cancelamento do pedido e aplicar as mesmas regras de estorno condicional de estoque.
- **FR-007**: O sistema DEVE assegurar que operações repetidas de cancelamento sobre um pedido já cancelado não causem múltiplos incrementos de estoque (idempotência de estorno).

### Key Entities

- **PurchaseOrder**: Representa a transação de compra. Atributos-chave: `id`, `user` (titular), `status` (`PENDING`, `PAID`, `PACKING`, `SHIPPED`, `DELIVERED`, `CANCELED`), `totalValue`, `deliveryAddress`, lista de `items`.
- **OrderItem**: Representa a linha do item comprado. Atributos-chave: `id`, `order`, `product`, `quantity`, `price` (snapshot do momento da compra).
- **Product**: Representa a mercadoria vendida. Atributos-chave: `id`, `name`, `sku`, `stock` (saldo físico decrementado/incrementado), `status` (`ACTIVE`, `INACTIVE`).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos cancelamentos efetuados em pedidos elegíveis (`PENDING`, `PAID`, `PACKING`) resultam na recomposição exata do estoque físico sem divergências numéricas.
- **SC-002**: 0% de pedidos com status `SHIPPED` ou `DELIVERED` conseguem ser cancelados via API de cancelamento.
- **SC-003**: A transação de cancelamento e recomposição de estoque de todos os itens executa em menos de 200ms em média sob carga nominal.
- **SC-004**: 100% das tentativas de cancelamento parcial de itens são impedidas pelo sistema.

---

## Assumptions

- O sistema opera no modelo Single-Tenant (loja única), sem separação por `tenantId`.
- Processos de logística reversa pós-entrega (devolução, troca ou RMA para pedidos já entregues) estão fora do escopo desta feature e serão tratados em especificação separada.
- O controle de estoque físico é baseado em saldo numérico inteiro persistido no banco de dados relacional.
- Autenticação e autorização utilizam o mecanismo JWT existente no ecossistema Spring Security.
