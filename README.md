[NOSSO VÍDEO](https://youtu.be/49kOjDBGJcU)

# Para executar o sistema:

    1° - Em um terminal, executar o comando 'runAllServers'.

    2° - Em outro terminal, executar os comandos:

    './runAdminServer.sh' e './runOrderServer.sh'.

    Isto inicializará, respectivamente, (2 de cada) os servidores:
        AdminPortalServer;
        OrderPortalServer;
    Todos serão executados em paralelo.
    
    3° - Em outro terminal, executar o comando './runAdminPortalClient.sh'.
    Isto inicializará o cliente AdminPortalCleint. É através desta janela
    que o usuário interagirá com o Portal de Administrador.
    
    4° - Em outro terminal, executar o comando './runOrderPortalClient.sh'.
    Isto inicializará o cliente OrderPortalClient. É através desta janela
    que o usuário interagirá com o Portal de Pedidos.

    5° - Ao final da execução do comando './runAllServers.sh', é possível
    executar o comando './maybe-kill-non-intellij-only-and-reset-db.sh', que limpa
    os processos java que estavam em uso, assim como limpa o banco de dados para
    garantir o funcionamento adequado do sistema entre runs.

# Execução via .jar

    1° - Não é possível executar puramente o sistema através do .jar gerado na 
    compilação, <terminar>

## Compilação

    Em um terminal execute o comando: `compile.sh`.
    Script responsável por compilar o sistema através do Maven, que faz uma limpeza
    (mvn clean) e, em seguida, compila o projeto (mvn compile).

### Erros ou Comportamentos Esperados

    <terminar>

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
    2° - GRADLE 3.10
    3° - Bibliotecas e dependências no pom.xml

    Da migração: em relação à primeira entrega do projeto, foi necessário migrar do
    Gradle para o Maven, uma vez que o Gradle apresentada comportamentos indesejados,
    tais como alto consumo de poder computacional e recompilações desnecessárias.

# O que foi coberto

    1° - Ratis para comunicação com os servidores e máquinas de estado determinísticas;
    2° - LevelDB para o Banco de Dados chave-valor;
    3° - 03 (três) réplicas por partição;
    4° - Caches locais para cada servidor;
    5° - Política de atualização que leva em consideração o instante em que ocorre
    a requisição em relação, o tempo de vida do dado já existente e o tempo decorrido
    entre ambos.
