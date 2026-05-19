# Link Cable Local

Este documento registra a nova direcao oficial para multiplayer local.

A abordagem anterior tentava usar TCP/Unix socket tambem para uso local. Ela funcionou em poucos casos simples, mas Tetris e Pokemon mostraram o problema estrutural: duas instancias independentes avancam em tempos diferentes, recebem snapshots atrasados do outro lado e podem acabar decidindo ao mesmo tempo que sao Player 1/master.

## Decisao

- Link local deve rodar em uma unica instancia do GBCEMU.
- A aplicacao deve abrir uma janela com Player 1 e Player 2 lado a lado.
- Os dois cores devem ser coordenados por uma sessao `Emulator` com dois `Console`.
- O cabo link deve ser um objeto compartilhado em memoria, conectando diretamente o `Serial` dos dois emuladores.
- Socket local deixa de ser objetivo.
- TCP fica para uma futura implementacao de netplay/lockstep, separada da primeira versao de link local.

## Modelo Alvo

```text
GBCEMU
  Emulator Session
    Console P1 -> Linked Window Left  -> Serial P1
            |                     |
            |                DirectLinkCable
            |                     |
    Console P2 -> Linked Window Right -> Serial P2
```

## Responsabilidades

### `Emulator`

- Criar e controlar uma lista de `Console`.
- Tickar os dois cores de forma coordenada em uma unica thread de sessao, sempre avancando o `Console` com menos ciclos de maquina acumulados e aplicando o throttle uma vez por frame da sessao.
- Garantir que pause, stop, restart e fechamento afetem a sessao inteira.
- Impedir rewind, save state e turbo enquanto a sessao link estiver ativa.
- Expor estado conjunto para debug, gerando um indice de dump da sessao mais dumps individuais de P1/P2.

### `Console`

- Representar um Game Boy fisico completo: CPU, PPU, APU, Bus, Cart, RAM, Serial, Timer, DMA/HDMA e Joypad.
- Nao possuir loop proprio; expor apenas `tick()`/passos de execucao para a sessao.
- Reportar quantos ciclos de maquina foram avancados em cada `tick()`, para o scheduler manter os consoles em lockstep mesmo quando instrucoes diferentes tem duracoes diferentes.
- Manter save state, rewind individual, debug e dump do hardware daquele console.

### `DirectLinkCable`

- Representar o cabo link como meio compartilhado de dados + clock.
- Receber sinais dos dois `Serial`.
- Completar uma transferencia de 8 bits quando um lado fornece clock e o outro esta armado em clock externo.
- Quando os dois lados selecionam clock interno, somente um lado dirige aquele byte: o lado que armou a transferencia primeiro vira o master efetivo, e P1 serve apenas como desempate.
- Detectar ou registrar dual-master sem mascarar o valor real de `SC` escrito pelo jogo.
- Nao depender de socket, polling de rede ou snapshots remotos.

### `Serial`

- Continuar sendo a interface de hardware vista pelo jogo (`SB`/`SC`).
- Gerar clock interno quando `SC bit 0 = 1`.
- Aguardar clock externo quando `SC bit 0 = 0`.
- Disparar interrupcao serial ao completar a transferencia.

### Controles

- Configuracoes separadas para Player 1 e Player 2.
- Entrada inicial por `KeyEventDispatcher` global da aplicacao, para que as duas janelas recebam input enquanto qualquer janela do GBCEMU estiver focada.
- Gamepad/Input4J fica como evolucao natural para permitir controles independentes, inclusive rumble.

## Fora Do Escopo Da Primeira Versao

- TCP.
- Unix domain socket.
- Netplay remoto.
- Infrared virtual.
- Sincronizacao por frame da PPU.
- Polling agressivo de socket no hot path.

## Referencia De Outros Emuladores

Consulta feita durante investigacao de Tetris/Pokemon escolhendo Player 1 nos dois lados:

- Pan Docs descreve a transferencia serial como clock + dados, com um lado fornecendo clock e o outro armado para clock externo.
- mGBA modela o SIO do GB com eventos de timing e callback de driver, nao como mensagens independentes de alto nivel entre dois processos.
- Emuladores com link local estavel normalmente coordenam as instancias localmente ou usam uma camada explicita de lockstep.

Conclusao pratica: primeiro implementar link local deterministico no mesmo processo. TCP deve ser tratado depois como netplay, nao como substituto do cabo local.
