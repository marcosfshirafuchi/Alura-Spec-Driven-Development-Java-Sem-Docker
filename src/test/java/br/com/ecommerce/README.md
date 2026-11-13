# Módulo: test (`src/test/java/br/com/ecommerce`)

## 1. Objetivo do Módulo
Fornecer a infraestrutura de garantia de qualidade, verificação de regressões e testes automatizados (unitários e de integração) para todos os componentes do sistema e-commerce.

---

## 2. Responsabilidade Principal
- Executar validações automatizadas de lógica de negócio e regras de domínio.
- Garantir a conformidade dos contratos de API REST (códigos HTTP, serialização de respostas, validações de payload).
- Testar a integridade transacional de operações críticas (checkout, concorrência de estoque e idempotência de pagamento).
- Validar as regras de segurança e autorização de endpoints (Spring Security / JWT).

---

## 3. Funcionalidades Existentes
No estado atual da base de código, o módulo possui apenas um teste de fumaça simbólico:

### `EcommerceApplicationTests.java`
```java
package br.com.ecommerce;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EcommerceApplicationTests {
    @Test
    void projectSkeleton() {
        assertTrue(true);
    }
}
```
- A classe não possui `@SpringBootTest`, não sobe o contexto Spring e não executa nenhuma asserção real além de `assertTrue(true)`.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- Potencialmente todos os módulos de `src/main/java`.

### Dependências Externas (configuradas no `pom.xml`)
- `org.springframework.boot:spring-boot-starter-test` (escopo `test`): Inclui JUnit 5 (Jupiter), AssertJ, Hamcrest, Mockito, JSONassert e Spring Test (`MockMvc`).
- `org.springframework.security:spring-security-test` (escopo `test`): Fornece suporte a testes de segurança, anotações `@WithMockUser`, `@WithAnonymousUser` e utilitários de autenticação JWT simulada.

---

## 5. Módulos Relacionados
- Todos os pacotes de produção: `config`, `controller`, `service`, `repository` e `model`.

---

## 6. Pontos de Entrada
- Execução da fase de testes do ciclo de vida Maven (`mvn test`).
- Test runner do JUnit 5 integrado à IDE.

---

## 7. Fluxos Importantes
- **Fluxo Atual**: Execução imediata do método `projectSkeleton()`, que retorna sucesso sem avaliar nenhum componente de software.
- **Fluxos Necessários para Spec-Driven Development [Hipótese / Requisito Futuro]**:
  1. Testes de Unidade de Domínio e Serviços (comportamento de `OrderService.checkout`, `CartService`, `PaymentService`).
  2. Testes de Integração de API com `MockMvc` cobrindo cenários de sucesso e erro (400, 401, 403, 404, 409).
  3. Testes de Concorrência Simulada com Múltiplas Threads testando débito simultâneo de estoque.

---

## 8. Arquivos Críticos
- `EcommerceApplicationTests.java`: Único arquivo de teste presente na suíte atual.

---

## 9. Observações Técnicas e Débitos Identificados
- **Débito Crítico: Cobertura de Testes Nula**: O sistema não possui nenhum teste unitário ou de integração cobrindo fluxos reais. Alterações nas camadas de serviço ou repositório podem introduzir quebras de regressão silenciosas.
- **Falta de Testes de Autenticação e Segurança**: Não há verificação automatizada garantindo que endpoints restritos (`/products` com método `POST`) realmente bloqueiem requisições sem o papel `ADMIN`.
- **Falta de Testes Transacionais**: O fluxo de checkout (`OrderService.checkout`) envolve múltiplas operações de banco e memória sem nenhum teste garantindo que o rollback ocorra em caso de exceção intermediária.
- **Diretriz Prioritária para SDD**: Antes de novas adições funcionais, a suíte de testes deve ser estruturada com cobertura dos fluxos centrais descritos no documento de Arquitetura.
