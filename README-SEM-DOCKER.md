# E-commerce Spring Boot — sem Docker

## Executar no IntelliJ IDEA
1. Abra a pasta `ecommerce-springboot` como projeto Maven.
2. Selecione JDK 21 (ou JDK 25) em Project SDK e recarregue o Maven.
3. Execute `br.com.ecommerce.EcommerceApplication` (Run).
4. A API responde em `http://localhost:8080/api/v1`.
5. Teste `GET http://localhost:8080/api/v1/products` ou `GET http://localhost:8080/api/v1/categories`.

## Banco H2
O banco fica em `./data/ecommerce.mv.db`, relativo ao diretório de execução. Não precisa instalar PostgreSQL. O console H2 está em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/ecommerce`, usuário `sa`, senha vazia). Uso apenas local.

## Mudanças e limitações
- Removidas as dependências PostgreSQL e Redis; incluído H2.
- Carrinhos ficam em memória e são perdidos ao reiniciar a aplicação.
- JWT access e refresh continuam sendo emitidos, mas refresh não é persistido, nem existe endpoint de renovação.
- O banco H2 mantém os registros após reiniciar, desde que o diretório de execução permaneça igual.
- O Dockerfile e docker-compose.yml foram removidos desta variante.
- O arquivo README.md original descreve a versão antiga; use este guia.
- Não utilizar esta configuração em produção.
