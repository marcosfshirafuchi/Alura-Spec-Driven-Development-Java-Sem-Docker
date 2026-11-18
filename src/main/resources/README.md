# Módulo: resources (`src/main/resources`)

## 1. Objetivo do Módulo
Centralizar as propriedades de configuração da aplicação em tempo de execução, parâmetros de infraestrutura (servidor web Tomcat, banco de dados H2, Hibernate JPA) e segredos de criptografia da aplicação no formato autônomo sem dependência de Docker.

---

## 2. Responsabilidade Principal
- Especificar parâmetros de conexão JDBC com o banco de dados embutido H2 baseado em arquivo (`./data/ecommerce.mv.db`).
- Ajustar estratégias de inicialização de schema do Hibernate (`ddl-auto: update`).
- Configurar portas de escuta e caminhos de acesso do console web do H2.
- Definir a chave secreta simétrica de assinatura e validação de tokens JWT.

---

## 3. Configurações Ativas (`application.yml`)

```yaml
server:
  port: ${PORT:8080}
spring:
  datasource:
    url: jdbc:h2:file:./data/ecommerce;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  h2:
    console:
      enabled: true
      path: /h2-console
app:
  jwt-secret: ${JWT_SECRET:change-this-development-only-secret-32-characters-minimum}
```

---

## 4. Esquema Relacional e Decisões de Arquitetura

1. **Arquitetura Single-Tenant (Loja Única)**:
   - As tabelas não possuem colunas de `tenant_id`. Todas as operações pertencem ao domínio global único da loja.
2. **Novas Tabelas e Relacionamentos Derivados**:
   - `product_categories`: Tabela associativa de junção para suportar o relacionamento muitos-para-muitos entre produtos e categorias.
   - `delivery_addresses`: Tabela ou colunas embutidas em `orders` para retenção imutável do endereço de entrega.
   - `carts` e `cart_items` *(Recomendação de Evolução)*: Tabelas relacionais para substituir o armazenamento em memória `ConcurrentHashMap`, mantendo o projeto autônomo sem exigir Docker ou Redis instalado na máquina.

---

## 5. Observações Técnicas e Débitos Identificados
- **Uso de `ddl-auto: update`**: Para ambientes corporativos, deve ser substituído por migrações versionadas via Flyway.
- **Risco de Segurança com Chave JWT Padrão**: A variável de ambiente `JWT_SECRET` deve ser sempre definida em ambientes de teste ou homologação para sobrescrever o valor padrão em texto claro.
