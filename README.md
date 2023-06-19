# Para executar o sistema:

    1° - Em um terminal, executar o comando 'runAllServers.sh'.

    que executam comandos como 

```
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.AdminPortalServer -Dlog4j.rootLogger=DEBUG &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.AdminPortalServer -Dexec.args="25507" -Dlog4j.rootLogger=DEBUG &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p1" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p2" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="0 p3" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p1" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p2" -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.distributedDatabase.RatisServer -Dexec.args="1 p3" -Dlog4j.rootLogger=DEBUG  &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.OrderPortalServer -Dlog4j.rootLogger=DEBUG   &
mvn exec:java -Dexec.mainClass=ufu.davigabriel.server.OrderPortalServer -Dexec.args="60553" -Dlog4j.rootLogger=DEBUG   &
```

    que deixam em segundo plano cada classe importante

    2° - Em outro terminal, executar o comando './runAdminPortalClient.sh'.
    Isto inicializará o cliente AdminPortalCleint. É através desta janela
    que o usuário interagirá com o Portal de Administrador.
    
    3° - Em outro terminal, executar o comando './runOrderPortalClient.sh'.
    Isto inicializará o cliente OrderPortalClient. É através desta janela
    que o usuário interagirá com o Portal de Pedidos.

    4° - Ao final da execução do comando './runAllServers.sh', é possível
    executar o comando './maybe-kill-non-intellij-only-and-reset-db.sh', que limpa
    os processos java que estavam em uso, assim como limpa o banco de dados para
    garantir o funcionamento adequado do sistema entre runs.

    Ainda assim é recomendado que veja se o seu intellij (ou aplicações java na sua máquina)
    está executando só e somente 2 processos.
    Se for mais, este script pode matá-los, se for menos, algum processo ficará vivo.

# Rodar testes automatizados

    Comente a linha @Ignore nos testes

    Rode mvn clean compile


    Para o do admin:
    - Execute só as réplicas assim como dito acima, ou o script `./runOnlyReplicas.sh`
    - Executávamos via intellij cada teste esperado

    Para o do order:
    - Execute todos servidores (menos clientes) `./runAllServers.sh`
    - Executávamos via intellij cada teste esperado


# Execução via .jar

    1° - Não é possível executar puramente o sistema através do .jar gerado na 
    compilação, mas para as réplicas é. Ele entrará em execução, mas provavelmente 
    dará um erro de IOException e abort de requisição GRPC. 
    Embora tenhamos configurado, a configuração normal do GRPC
    conflita com os plugins de geração de dependência usados na disciplina como
    Raft.

    Novamente passamos muita dor de cabeça numa exceção de IO (provavelmente interno do JAR pelo 
    debug e pelo site que encontramos dizendo que a Google não suporta tão bem o Maven) e que
    simplesmente não acontecia rodando SEM o jar (pelo intellij) e descobrimos o `mvn exec`
    que resolveu, mas se quiser verificar o que estamos dizendo.

## Compilação

    Em um terminal execute o comando: `compile.sh`.
    Script responsável por compilar o sistema através do Maven, que faz uma limpeza
    (mvn clean) e, em seguida, compila o projeto (mvn compile).

    `mvn clean compile`

### O que faltou
    
    O sistema tem uma cache com TTL, então pode haver inconsistência se for muito rápido,
    mas não causal. Pode haver perda de escrita então ou dirty read.
    

# Para executar os testes automatizados:

    1° - Para os testes especificamente relacionados ao Portal Administrativo,
    é necessário, apenas, executar o './runOnlyReplicas'.

    2° - Para os testes especificamente relacionados ao Portal de Pedidos,
    é necessário executar o './runAllServers', pois o Portal de Pedidos
    precisa se comunicar com o Portal Administrativo.

    3° - Para acessar os testes, referir-se aos arquivos contidos na pasta:
    './src/test/java'. Recomendamos utilizar o Intellij.

# VERSÕES

    1° - JAVA 17.0.8
    2° - MAVEN 3.8.7
    3° - Bibliotecas e dependências no pom.xml

    Da migração: em relação à primeira entrega do projeto, foi necessário migrar do
    Gradle para o Maven, uma vez que o Gradle apresentada comportamentos indesejados,
    tais como alto consumo de poder computacional e recompilações desnecessárias.

# O que foi coberto

    1° - Ratis para comunicação com os servidores e máquinas de estado determinísticas;
    2° - LevelDB para o Banco de Dados chave-valor;
    3° - 03 (três) réplicas por partição;
    4° - Caches locais para cada servidor;
    5° - Política de atualização CAUSAL que leva em consideração o instante em que ocorre
    a requisição em relação, o tempo de vida do dado já existente e o tempo decorrido
    entre ambos.
