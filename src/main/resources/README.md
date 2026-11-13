# Módulo: resources (`src/main/resources`)

## 1. Objetivo do Módulo
Centralizar as propriedades de configuração da aplicação em tempo de execução, parâmetros de infraestrutura (servidor web, banco de dados H2, Hibernate JPA) e segredos de criptografia da aplicação.

---

## 2. Responsabilidade Principal
- Definir portas de escuta e configurações de rede do servidor embutido.
- Especificar parâmetros de conexão JDBC com o banco de dados embutido H2 baseado em arquivo.
- Ajustar estratégias de inicialização de schema do Hibernate (`ddl-auto`).
- Definir a chave secreta de assinatura JWT para desenvolvimento e produção.
- Habilitar e configurar o caminho de acesso ao console web do H2.

---

## 3. Funcionalidades Existentes

### 3.1. `application.yml`
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

- **`server.port`**: Porta HTTP da API, assumindo `8080` como padrão ou o valor da variável de ambiente `PORT`.
- **`spring.datasource`**:
  - URL JDBC: `jdbc:h2:file:./data/ecommerce;DB_CLOSE_ON_EXIT=FALSE`, indicando persistência em arquivo local no subdiretório `./data/` a partir do diretório de execução da aplicação.
  - Driver: `org.h2.Driver`.
  - Credenciais locais: usuário `sa`, senha vazia.
- **`spring.jpa`**:
  - `hibernate.ddl-auto: update`: O Hibernate cria ou altera as tabelas automaticamente na inicialização com base nas entidades JPA.
  - `open-in-view: false`: Desativa o padrão *Open Session in View*, forçando o fechamento da sessão Hibernate ao término da transação do serviço para evitar lazy loading não intencional durante a renderização de views.
- **`spring.h2.console`**: Console web administrativo do banco H2 ativo na rota `/h2-console`.
- **`app.jwt-secret`**: Chave de 256 bits para assinatura HMAC de tokens JWT, com valor padrão de desenvolvimento e suporte a override via variável de ambiente `JWT_SECRET`.

---

## 4. Dependências Internas e Externas
### Dependências Internas
- Injetada nas classes Spring via anotações `@Value("${app.jwt-secret}")` (em `SecurityConfig.java`).

### Dependências Externas
- `com.h2database:h2`: Driver de banco de dados JDBC.
- `org.springframework.boot:spring-boot-starter-data-jpa`: Mecanismo que processa as chaves `spring.datasource.*` e `spring.jpa.*`.

---

## 5. Módulos Relacionados
- `br.com.ecommerce.config.SecurityConfig`: Lê a chave de assinatura JWT configurada neste arquivo.
- `br.com.ecommerce.repository` e `model`: Inicializam suas tabelas e conexões de acordo com os parâmetros definidos neste módulo.

---

## 6. Pontos de Entrada
O arquivo `application.yml` é carregado automaticamente pelo Spring Boot durante o estágio de inicialização do `ApplicationContext`.

---

## 7. Fluxos Importantes
- **Criação e Conexão com o Arquivo de Banco**: Na subida da aplicação, o driver H2 verifica a existência do arquivo `./data/ecommerce.mv.db`. Se não existir, ele cria o arquivo e o Hibernate executa as instruções DDL derivadas das entidades anotadas com `@Entity`.

---

## 8. Arquivos Críticos
- `application.yml`: Único arquivo de propriedades do projeto.

---

## 9. Observações Técnicas e Débitos Identificados
- **Risco de Segurança com Segredo Padrão**: A chave JWT padrão (`change-this-development-only-secret-32-characters-minimum`) está exposta em texto claro no código fonte. Em ambientes que não definirem a variável `JWT_SECRET`, tokens poderão ser forjados por agentes maliciosos que conheçam o código.
- **Uso de `ddl-auto: update`**: O Hibernate altera tabelas em tempo de execução sem registro de histórico ou capacidade de rollback seguro. Inadequado para produção; deve ser substituído por ferramentas como Flyway ou Liquibase.
- **Caminho Relativo do Banco H2 (`./data/ecommerce`)**: O caminho é relativo ao diretório em que o comando de execução é disparado. Se o usuário rodar a aplicação a partir de pastas distintas, diferentes bancos de dados locais serão criados.
- **Console H2 Habilitado**: O console `/h2-console` está ativo sem restrição adicional de rede além do Spring Security. Em ambiente de produção deve ser desabilitado (`enabled: false`).
