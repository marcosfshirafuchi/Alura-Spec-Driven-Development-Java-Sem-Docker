# Guia Oficial de Ambiente Local e Execução Sem Docker

> [!IMPORTANT]
> **DIRETRIZ OBRIGATÓRIA**: O projeto `ecommerce-springboot` opera exclusivamente de forma **local e autônoma sem Docker**. É expressamente proibido o uso de containers Docker, Docker Compose ou Testcontainers. Todas as execuções, testes e integrações utilizam o Java 21 nativo, Maven e banco de dados H2 embarcado.

---

## 1. Especificações Técnicas e Versões

| Componente | Versão Homologada | Finalidade |
| :--- | :--- | :--- |
| **Java (JDK)** | 21 (LTS) — *Oracle JDK / Eclipse Temurin* | Runtime principal da aplicação |
| **Spring Boot** | 3.5.6 | Framework base (Web, Data JPA, Security) |
| **Maven** | 3.9+ | Gerenciador de compilação e dependências |
| **Banco de Dados** | H2 Database 2.3.232 (Embarcado) | Banco relacional embutido na JVM |
| **Node.js / npm** | Node v20+ / npm v10+ (Host) | Executor de testes Postman via Newman CLI |
| **Porta da Aplicação**| `8080` (HTTP) | Porta padrão do servidor web Tomcat embutido |
| **Console H2** | `/h2-console` | Interface web para administração do banco H2 |

---

## 2. Configurações de Banco de Dados Local (Sem Docker)

O projeto não requer instalação de SGBDs externos (como PostgreSQL, MySQL ou Oracle). Ele utiliza o **H2 Database**:

### 2.1. Ambiente de Execução Local (`src/main/resources/application.yml`)
- **Tipo**: H2 baseado em arquivo persistido localmente.
- **URL JDBC**: `jdbc:h2:file:./data/ecommerce;DB_CLOSE_ON_EXIT=FALSE`
- **Driver**: `org.h2.Driver`
- **Usuário**: `sa`
- **Senha**: *(vazia)*
- **DDL Strategy**: `hibernate.ddl-auto: update`
- **Inicialização de Seed**: `spring.sql.init.mode: always` apontando para `classpath:seed-data.sql` com `defer-datasource-initialization: true`.

### 2.2. Ambiente de Testes Automatizados (`src/test/resources/application.yml`)
- **Tipo**: H2 100% em memória (*In-Memory*).
- **URL JDBC**: `jdbc:h2:mem:ecommerce_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- **DDL Strategy**: `hibernate.ddl-auto: create-drop` (limpeza automática a cada ciclo de execução).
- **Seed de Teste**: `classpath:seed-data.sql`.

---

## 3. Variáveis de Ambiente

As propriedades do sistema utilizam valores padrão seguros para desenvolvimento local, podendo ser sobrescritas por variáveis de ambiente:

| Variável | Valor Padrão Local | Descrição |
| :--- | :--- | :--- |
| `PORT` | `8080` | Porta TCP do servidor HTTP Tomcat |
| `JWT_SECRET` | `change-this-development-only-secret-32-characters-minimum` | Chave simétrica HMAC-SHA256 para assinatura dos tokens JWT |

> [!CAUTION]
> Nunca versione credenciais, tokens JWT de produção ou segredos no repositório. Em ambientes produtivos, `JWT_SECRET` deve ser fornecido via cofre de segredos ou variáveis de ambiente externas seguras.

---

## 4. Dependências Externas Necessárias

Nenhum software além do JDK 21 e Maven é obrigatório para compilar e executar o sistema:
- O carrinho de compras é mantido em memória via `ConcurrentHashMap` (`CartService`).
- O banco de dados roda dentro do próprio processo da JVM.
- Para testes do Postman via terminal, utiliza-se o `npx --yes newman` (requer Node.js no host).

---

## 5. Procedimentos de Execução da Aplicação

### 5.1. Iniciar a Aplicação Localmente
No terminal PowerShell ou Bash na raiz do projeto:
```powershell
mvn spring-boot:run
```
Ou executando a classe principal `br.com.ecommerce.EcommerceApplication` diretamente pela IDE (IntelliJ IDEA, VS Code, Eclipse).

A aplicação confirmará a inicialização com a linha de log:
`Started EcommerceApplication in X.XX seconds (process running for X.XX)`

### 5.2. Encerrar a Aplicação
- No terminal: pressione `Ctrl + C`.
- Caso esteja rodando em background (PowerShell):
  ```powershell
  Get-Process -Name java | Stop-Process -Force
  ```

---

## 6. Procedimentos para Executar Testes Automatizados

### 6.1. Suíte Completa de Testes (Unitários e Integração)
Execute o ciclo Maven completo de compilação, verificação e empacotamento:
```powershell
mvn clean verify
```

### 6.2. Execução de Testes Específicos
- **Apenas testes unitários de cancelamento e estoque:**
  ```powershell
  mvn test -Dtest=OrderServiceTest
  ```
- **Apenas testes de integração do controller REST:**
  ```powershell
  mvn test -Dtest=OrderControllerIntegrationTest
  ```
- **Apenas sincronização com pagamento:**
  ```powershell
  mvn test -Dtest=PaymentSyncTest
  ```

---

## 7. Procedimentos para Executar Testes do Postman (Caixa Preta)

A suíte Postman valida o novo endpoint RESTful `DELETE /order/{id}` e todos os cenários de cancelamento, integridade de estoque, governança RBAC e idempotência.

### 7.1. Execução Automatizada via Newman (Terminal)
Com a aplicação em execução na porta 8080:
```powershell
npx --yes newman run postman/order-cancellation.postman_collection.json -e postman/ecommerce-local.postman_environment.json
```

### 7.2. Execução via Interface Gráfica do Postman
1. Abra o Postman Desktop.
2. Clique em **Import** e selecione os arquivos:
   - `postman/order-cancellation.postman_collection.json`
   - `postman/ecommerce-local.postman_environment.json`
3. Selecione o ambiente **E-Commerce Local Environment** no canto superior direito.
4. Abra a coleção e clique em **Run Collection**.

---

## 8. Problemas Identificados e Respectivas Soluções

### Problema 1: `JdbcSQLDataException: Value not permitted for column: SHIPPED`
- **Causa**: O arquivo de banco persistido `./data/ecommerce.mv.db` foi criado com versão anterior do enum `OrderStatus` (que continha apenas `PENDING`, `PAID`, `CANCELED`). O Hibernate com `ddl-auto: update` não altera constraints de enums pré-existentes no H2.
- **Solução**: Exclua os arquivos de banco local `./data/ecommerce*` para permitir que o Hibernate recrie o schema completo com os novos valores de enum (`PACKING`, `SHIPPED`, `DELIVERED`):
  ```powershell
  Remove-Item -Path data\ecommerce* -Force -ErrorAction SilentlyContinue
  ```

### Problema 2: `HttpMessageNotWritableException: failed to lazily initialize a collection: PurchaseOrder.items`
- **Causa**: Na chamada idempotente de cancelamento, a entidade `PurchaseOrder` já cancelada era retornada diretamente sem iterar sobre `order.items`. Como `open-in-view` está desativado (`false`), a serialização Jackson falhava fora da sessão Hibernate.
- **Solução**: No `OrderService.cancel`, invocou-se `order.items.size()` dentro do método anotado com `@Transactional`, garantindo a inicialização da coleção de itens antes do encerramento da sessão Hibernate.

### Problema 3: Falha de Autenticação com Senha da Seed no Postman (401 Unauthorized)
- **Causa**: O hash BCrypt inserido manualmente no script SQL divergia da senha informada na requisição de login (`123456`).
- **Solução**: O hash BCrypt homologado para a senha `123456` foi gerado via `BCryptPasswordEncoder` e fixado nos scripts de seed (`$2a$10$bjodOAeFNvIs2hrAjEtE6eBTXAnDRYF69Vd7fjg5ezKdfGcvAywFi`).

---

## 9. Manutenção Contínua e Diretrizes para Futuras Sessões
Toda alteração de contratos, portas, variáveis de ambiente ou dependências deve ser obrigatoriamente refletida neste documento e no arquivo [`AGENTS.md`](file:///c:/Users/use/Documents/Projetos%20Java/ecommerce-springboot-sem-docker/ecommerce-springboot/AGENTS.md).
