# Módulo: test (`src/test/java/br/com/ecommerce`)

## 1. Objetivo do Módulo
Fornecer a infraestrutura de garantia de qualidade, verificação de regressões e testes automatizados (unitários e de integração) para todos os componentes do sistema e-commerce, orientados para desenvolvimento baseado em especificações (Spec-Driven Development).

---

## 2. Responsabilidade Principal
- Validar as regras de negócio de pedidos, baixa e estorno condicional de estoque.
- Garantir a conformidade dos contratos REST e códigos HTTP (`200`, `201`, `204`, `400`, `401`, `403`, `409`).
- Testar as travas de autorização e segurança RBAC (`USER` vs `ADMIN`).
- Validar a integridade das composições de agregação (`PurchaseOrder` compondo `DeliveryAddress` e `OrderItem`).

---

## 3. Matriz de Cenários Prioritários para Implementação

Com base nas definições consolidadas na entrevista de alinhamento, a suíte de testes deve cobrir obrigatoriamente os seguintes cenários:

### 3.1. Regras de Cancelamento e Estorno de Estoque
1. **Cancelamento em Estados Permitidos (`PENDING`, `PAID`, `PACKING`)**:
   - Dado um pedido em status elegível e itens com quantidades debitadas do produto;
   - Ao executar o cancelamento (`OrderService.cancel`);
   - Deve alterar o status para `CANCELED` e **restaurar integralmente as quantidades no estoque de cada produto**.
2. **Bloqueio de Cancelamento Pós-Envio (`SHIPPED`, `DELIVERED`)**:
   - Dado um pedido com status `SHIPPED` ou `DELIVERED`;
   - Ao tentar cancelar o pedido;
   - Deve lançar `ApiException(400 Bad Request)` com mensagem explicativa e **manter o estoque inalterado**.
3. **Proibição de Cancelamento Parcial**:
   - Garantir que a API rejeite qualquer tentativa de remover ou estornar itens individualmente de um pedido fechado.

### 3.2. Transições e Permissões de Status do Pedido
1. **Avanço Logístico Restrito a `ADMIN`**:
   - Requisição de cliente comum (`ROLE_USER`) tentando atualizar para `PACKING` ou `SHIPPED` deve retornar `403 Forbidden`.
   - Requisição de administrador (`ROLE_ADMIN`) deve atualizar o status com sucesso (`200 OK`).
2. **Confirmação de Recebimento pelo Consumidor**:
   - Cliente titular marcando seu próprio pedido como `DELIVERED` deve ser aceito.
   - Cliente tentando marcar pedido de outro usuário como `DELIVERED` deve retornar `403 Forbidden`.

### 3.3. Governança e Segurança de Contas de Usuários
1. **Bloqueio de Auto-Exclusão**:
   - Cliente autenticado chamando `DELETE /api/v1/users/{id}` (mesmo para o seu próprio ID) deve receber `403 Forbidden`.
2. **Exclusão Administrativa**:
   - Administrador autenticado chamando `DELETE /api/v1/users/{id}` deve excluir com sucesso (`204 No Content`).
3. **Alteração de Senha**:
   - Usuário autenticado alterando sua própria senha fornecendo senha atual correta deve obter sucesso (`200 OK`) e validar o novo hash BCrypt.

### 3.4. Catálogo e Visibilidade
1. **Remoção Imediata de Inativos**:
   - Produto com `status = INACTIVE` não deve constar na listagem pública `GET /api/v1/products`.
2. **Múltiplas Categorias**:
   - Produto associado a duas categorias deve ser retornado ao filtrar por qualquer uma delas.

### 3.5. Checkout e Endereço de Entrega
1. **Composição de `DeliveryAddress`**:
   - Validar que o pedido gerado contenha todos os atributos do endereço informados na requisição de checkout.

---

## 4. Dependências de Testes (configuradas no `pom.xml`)
- `org.springframework.boot:spring-boot-starter-test`: JUnit 5, AssertJ, Mockito, Spring Test (`MockMvc`).
- `org.springframework.security:spring-security-test`: Suporte a simulação de contextos JWT (`@WithMockUser`, `@WithMockJwt`).

---

## 5. Observações Técnicas e Débitos Identificados
- **Suíte Atual Esquelética**: `EcommerceApplicationTests` possui apenas um teste `assertTrue(true)` simbólico.
- **Próximo Passo**: Criar classes dedicadas `OrderServiceTest`, `CatalogServiceTest`, `OrderControllerIntegrationTest` e `UserControllerSecurityTest` cobrindo a matriz acima descrita.
