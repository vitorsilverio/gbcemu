# GBC EMU Roadmap

Este documento e a fonte unica de metas do emulador. Ele substitui listas soltas de tarefas pequenas e mantem o foco em funcionalidades maiores, compatibilidade real e ferramentas de debug que ajudam tanto o uso manual quanto a investigacao via Codex.

## Direcao

- Priorizar Game Boy Color.
- Manter o emulador utilizavel fora do IntelliJ.
- Evitar features de debug que deixem o runtime lento quando fechadas.
- Tratar save state, rewind e debug como infraestrutura central.
- Validar compatibilidade com test ROMs e jogos reais, nao apenas por impressao visual.

## Concluido / Base Atual

- Janela unica de emulador sem launcher separado.
- Menu para abrir ROM, configurar BIOS padrao, pausar, retomar e parar.
- Argumentos de linha de comando: `--rom`, `--bios`, `--save-file`, `--headless`, `--skip-bios`, `--no-save`, `--no-bios`.
- Sem BIOS configurada, o emulador usa `--skip-bios` automaticamente.
- Configuracoes persistentes para tela, som, rewind e teclado.
- Remapeamento de teclado.
- Overlay visual para `PLAY`, `PAUSE`, `STOP` e `REW`.
- Save states por jogo e por slot (`.sa0`, `.sa1`, `.saN`) com metadata, frame, PC e preview.
- Rewind inicial em memoria por snapshots intervalados.
- GameShark com UI para colar listas grandes de codigos.
- Debug separado por area:
  - CPU/disassembly.
  - Memoria e bancos.
  - PPU/tiles/tile maps/paletas.
  - Audio/canais.
  - Cart/MBC.
- Disassembler em tabela com cache por memoria/banco e breakpoints por PC.
- Debug de audio por canal, com mute/volume individual, master/stereo e trace opcional de writes da APU.
- Throttle por deadline acumulado, corrigindo starvation de audio em Windows.
- Otimizacoes de hot path da PPU:
  - framebuffer cacheado;
  - escrita direta no buffer da imagem;
  - candidatos de sprite preparados por scanline;
  - reducao de alocacoes por pixel.
- Save state refatorado para estados tipados, sem `@Savable`, `Snapshot(Map)` ou restauracao por reflexao.
- Cart RAM separada da ROM e exposta como `MemoryBank`.
- APU separada em componentes menores com estado explicito.

## Usabilidade

- [x] Configuracoes em abas.
  - [x] Geral.
  - [x] Graficos.
  - [x] Som.
  - [x] Controle.
  - [x] Mover configuracao de BIOS padrao para a janela de configuracoes.

- [ ] Gamepad.
  - Usar `input4j` ou alternativas de preferencia que não adicionem dependencia a JNI.
  - Detectar controles.
  - Mapear botoes.
  - Persistir perfil por controle quando possivel.
  - Suportar rumble para cartuchos/jogos compativeis.

- [ ] Configuracao de audio mais completa.
  - Latencia/buffer.
  - Device de audio.
  - Perfil de filtro de saida.

- [ ] Rewind continuo.
  - [x] Restaurar enquanto o usuario segura uma tecla/botao.
  - [x] Evitar custo quando rewind estiver desativado.
  - Avaliar snapshots menores ou delta compression.

- [ ] Turbo temporario.
  - [x] Acelerar enquanto uma tecla configuravel estiver pressionada.
  - [x] Multiplicador configuravel.
  - [x] Evitar que o sink de audio limite a velocidade durante turbo.
  - [x] Modo toggle alem de segurar tecla.
  - [x] Frameskip durante turbo para tentar passar de 3x.
  - [ ] Investigar turbo agressivo/performance acima de 3.5x sem comprometer compatibilidade.
  - [ ] Avaliar alternativa futura para audio em pitch acelerado durante turbo.

- [ ] Link cable.
  - [x] Suportar TCP sockets para conexao via rede.
  - [x] Suportar Unix domain sockets para conexao local simples, sem bloqueio de firewall.
  - [x] Melhorar UI para conectar/desconectar e indicar estado atual.
  - [x] Persistir ultima configuracao usada de local/TCP, host/guest, path, host e porta.
  - [x] Expor estado serial/link em debug: `SB`, `SC`, clock interno/externo, transferencia ativa, aguardando resposta.
  - [x] Expor estado conectado/hospedando no dump de debug do link.
  - [x] Modelo basico master (clock interno) + slave (clock externo) via `Serial` + byte no socket.
  - [x] Validar que slave nao completa transferencia sozinho sem clock externo (`SerialTest`).
  - Validado manualmente: trocas Pokemon Red <-> Silver na maior parte das vezes com um lado master e outro slave.
  - Sincronizacao suficiente para trocas e batalhas.
  - Plano de implementacao (seguir a ordem 1 → 6; nao pular para Fase 5 antes de 1–4):
    - [x] **Fase 1 — Camada `LinkCable` (simular o cabo).**
      - [x] Inserir entre `Serial` e `Multiplayer`: `Serial` nao chama `send()` direto no socket.
      - [x] `Serial` reporta estado (`SB`, `SC`, transferencia ativa, clock interno/externo, byte de saida) via `SerialLinkSnapshot`.
      - [x] Hub devolve eventos: par pronto, byte recebido, clock do parceiro (Fase 2); abort futuro se necessario.
      - [x] Regra: so clock interno avanca contador e inicia troca; clock externo so completa com clock/byte do parceiro (`Serial` + `InMemoryLinkCablePair` nos testes).
    - [x] **Fase 2 — Protocolo de link (substituir byte solto no socket).**
      - [x] Frames versionados (`HELLO`, `STATE`, `TRANSFER_REQUEST`, `TRANSFER_RESPONSE` em `LinkProtocol`).
      - [x] `STATE` sincroniza `SC bit 7` remoto; troca nao exige os dois prontos no mesmo instante (handshake Pokemon).
      - [x] Modelar troca como master inicia byte (`TRANSFER_REQUEST`) + slave responde (`TRANSFER_RESPONSE`).
      - [x] **Consolidacao:** Protocolo agora e estritamente baseado em frames; bytes soltos sao descartados para evitar conflito com MAGIC (`0x7C`).
    - [x] **Fase 3 — Eleicao / arbitragem master-slave.**
      - [x] Corrigir cenario em que os dois jogos ficam com clock interno (`SC` bit 0 = 1).
      - [x] Hub usa host/guest da rede para decidir quem emula o clock; nao reescrever `SC` no `write()` do jogo.
      - [x] **Update:** Arbitragem dinamica no `read()` do `SC` e no `tick()` do Serial para garantir compatibilidade com Pokemon e Tetris.
      - [x] Validado via `SerialLinkIntegrationTest.dualMasterArbitration`.
    - [x] **Fase 4 — `LinkSync` para trades e batalhas.**
      - [x] Barreira leve apos IRQ serial nos dois lados (nao lockstep frame-a-frame no inicio).
      - [x] Sincronizacao de frames baseada em `Ppu.frameNumber` para evitar drift entre instancias.
      - [x] Barreira de 2 frames de diferenca implementada em `LinkCable.tick()`.
      - [x] Suficiente para trocas e batalhas sem pausar o emulador inteiro entre bytes.
    - [ ] **Fase 5 — I/O do transporte (`Multiplayer`), incluindo `drainReceive`.**
      - **`drainReceive`:** apos `send()` do master, loop non-blocking em `read()` ate esvaziar buffer e entregar ao `Serial` na hora (nao esperar `tick()` com intervalo 4096).
      - **Polling adaptativo:** intervalo grande quando idle (conectado sem transferencia quente); intervalo curto ou drain apenas em estado hot (`transferActive`, `masterWaitingResponse`, slave aguardando clock).
      - Nao fazer poll a cada poucos ciclos durante toda a sessao (causa lentidao); clock CGB rapido (128 ciclos) exige resposta rapida so na janela da troca.
      - `drainReceive` e polling adaptativo entram aqui, apos o hub/protocolo/sync; o hub chama `drainReceive` no mesmo ponto em que hoje o master faz `send()`.
    - [ ] **Fase 6 — Testes.**
      - Unit: dois `Serial` + `InMemoryLinkCable`, master/slave, dual-master com hub.
      - Integracao: duas instancias headless; regressao Pokemon Red/Silver.

- [x] Saves `.sav` intercambiaveis.
  - [x] Passar a salvar RAM externa em formato bruto, igual outros emuladores.
  - [x] Manter leitura dos saves antigos com magic number, versao, tamanho e bytes extras.
  - [x] Ao carregar save antigo com sucesso, salvar novamente no formato bruto na proxima gravacao.
  - [x] Evitar perder progresso existente durante a migracao.

- [x] Offset manual de RTC.
  - [x] Configurar offset de horas, minutos e segundos.
  - [x] Permitir testar eventos dependentes de horario sem alterar o relogio do sistema.
  - [x] Aplicar em cartuchos com RTC, especialmente MBC3.
  - [x] Persistir offset nas configuracoes, nao no save state nem no `.sav`.

- [ ] Infrared com hardware real.
  - Criar camada de transporte.
  - Integrar Arduino/dispositivo serial.
  - Testar comunicacao com Game Boy Color real.

## Debug

- [x] Watchpoints.
  - [x] Breakpoint por leitura/escrita de endereco.
  - [x] Breakpoint por valor simples opcional.
  - [x] Desenhar para custo zero quando nenhum watchpoint estiver ativo.

- [x] Step/debug controls.
  - [x] Step instruction.
  - [x] Step frame.
  - [x] Step scanline.
  - [x] Run until VBlank/HBlank.
  - Exige arquitetura leve para nao repetir a lentidao da tentativa inicial.

- [x] Memory editor seguro.
  - [x] Nao usar apenas `bus.write` como padrao, porque dispara efeitos colaterais e falha em ROM/areas bloqueadas.
  - [x] Modo `hardware write`: via bus, explicitamente com efeitos colaterais.
  - [x] Modo `raw bank edit`: via `MemoryBank.writeBank`, limitado a memorias editaveis como WRAM, VRAM, HRAM e cart RAM.
  - [x] Permitir inicialmente apenas quando pausado.

- [x] Export de debug para Codex.
  - [x] JSON inicial da janela de CPU em `target/debug-cpu-window.json`.
  - [x] Incluir CPU, PPU basica, motivo de breakpoint e tabela atual do disassembler.
  - [x] Incluir interrupcoes, timer, cart/MBC e resumo dos bancos selecionados.
  - [x] Incluir serial.
  - [x] Incluir amostra pequena do banco atual de cada `MemoryBank`.
  - [x] Export JSON da janela de memoria com mapa, regiao visivel e amostras maiores dos bancos atuais.
  - [x] Export JSON da janela de PPU com registradores, frame stats, paletas, VRAM e OAM.
  - [x] Export JSON da janela de Cart/MBC com propriedades, mapper state e amostras de ROM/RAM.
  - [x] Utilitario comum para JSON de debug.
  - [x] Dump bundle pelo menu `Debug`, sem precisar abrir janelas individuais.
  - [x] Incluir disassembly forward no dump bundle.
  - [x] Incluir DMA, HDMA, registradores CGB e memory map no dump bundle.
  - [x] Incluir metadata de execucao no dump bundle.
  - [x] Exportar dumps completos de memoria por regiao/banco quando solicitado.
  - [x] Incluir screenshot do frame atual no dump bundle.

- [x] Disassembler avancado.
  - [x] Decodificar operandos e destinos de jumps/calls de forma mais rica.
  - [x] Invalidar cache quando memoria executavel ou banco relevante mudar.

## Compatibilidade CGB

### Ja Existe Base

- KEY0/KEY1 e troca de velocidade via `STOP`.
- Timer e serial com consideracao inicial de double speed.
- VRAM bank (`VBK`) e WRAM bank (`SVBK`).
  - [x] `VBK` le bits nao usados como 1.
  - [x] `SVBK` le bits nao usados como 1 e banco 0 mapeia banco 1.
- CGB palettes (`BGPI/BGPD`, `OBPI/OBPD`).
- Registradores CGB nao documentados (`FF72`-`FF75`).
  - [x] Latch dedicado para `FF72`-`FF75`.
- Atributos CGB de tile map: banco, paleta, flip e prioridade.
- OAM com atributos CGB: banco, paleta, flip e prioridade.
- HDMA/GDMA inicial.
- Infrared register (`FF56`) inicial.
  - [x] Mascaras de leitura/escrita do RP.
- Object priority mode (`OPRI`).
- [x] PCM registers (`FF76/FF77`) para saida digital da APU.
- MBC1, MBC3 com RTC, MBC5 e RAM externa.

### Faltante / Incerto

- [ ] `cgb_sound`.
  - Passar nos testes individuais.
  - Revisar frame sequencer, power on/off da APU, wave channel, DAC e mascaras de leitura.
  - Separar compatibilidade de registradores de qualidade do output para host.

- [ ] Timing CGB.
  - Revisar `cgb_timing`.
  - Confirmar double speed em CPU, timer, serial, PPU, DMA, HDMA e APU.
  - Garantir que componentes que nao dobram no CGB continuem no clock correto.

- [x] HDMA/GDMA completo.
  - [x] HBlank HDMA transfere no maximo um bloco de `$10` bytes por HBlank.
  - [x] HBlank HDMA nao transfere durante VBlank.
  - [x] Bloquear CPU durante GDMA e durante o bloco ativo de HBlank HDMA.
  - [x] Confirmar comportamento basico de cancelamento de HBlank HDMA e leitura de blocos restantes.
  - [x] Validar origem/destino, mascaras e leitura de `HDMA5`.

- [x] Bloqueios de acesso CGB.
  - [x] VRAM durante mode 3.
  - [x] OAM durante mode 2/3.
  - [x] Paletas durante modos bloqueados.
  - [x] Wave RAM durante CH3 ativo.
  - [x] `FF46/DMA` le o ultimo byte alto de origem escrito.

- [ ] PPU CGB edge cases.
  - Prioridade BG/window/sprite em CGB e modo compatibilidade DMG.
  - Window edge cases (`WX`, `WY`, reinicio por linha).
  - Penalidades de fetch e impacto de sprites/window.
  - OAM bug apenas se afetar CGB real ou jogos CGB.

- [ ] Paletas e boot behavior.
  - [x] `--skip-bios` usa registradores pos-BIOS CGB para ROMs CGB-compatible.
  - [x] `--skip-bios` usa registradores pos-BIOS CGB em compatibilidade DMG para ROMs DMG-only.
  - [x] `--skip-bios` aplica a ordem basica da BIOS CGB para `KEY0`/`OPRI` antes de desmapear `FF50`.
  - [x] `KEY0` trava quando a BIOS e desmapeada em `FF50`.
  - [x] `FF50` desmapeia a BIOS apenas em escrita nao-zero e uma unica vez.
  - [x] Sem BIOS configurada, abrir ROM usa skip-bios por padrao.
  - [x] `--skip-bios` aplica defaults estaveis de hardware CGB em `$0100` (`LCDC`, `BGP`, scroll/window, `SC`, `IF`, `IE`).
  - Confirmar estado pos-BIOS completo dos registradores de hardware.
  - Confirmar mapeamento RGB555/BGR555 e conversao para RGB host.
  - Confirmar comportamento de jogos DMG rodando em modo CGB.
  - Separar no PPU o modo de CPU/compatibilidade DMG (`KEY0`) do uso de paletas CGB.
    - No CGB real em compatibilidade DMG, as paletas CGB continuam ativas; `BGP`, `OBP0` e `OBP1` indexam as cores CGB escolhidas pela BIOS.
    - [x] Renderizacao principal usa paleta CGB mesmo em compatibilidade DMG.
    - [x] Debug de tilemap usa paleta CGB mesmo em compatibilidade DMG.
    - Tile viewer cru ainda mostra indices em escala de cinza porque nao tem contexto de tilemap/paleta.
  - Implementar/validar a tabela de compatibilidade da BIOS CGB para colorizacao automatica de jogos DMG.
    - [x] Expor no `CartHeader` licensee codes e checksum de titulo usados pelo algoritmo da BIOS CGB.
    - [x] Implementar seletor de ID de paleta por checksum/licenca e desempate pela quarta letra.
    - [x] Mapear ID selecionado para grupo e offsets de palavras de cor OBJ0/OBJ1/BG.
    - [x] Tabela de cores RGB555 da BIOS CGB exposta por offset de palavra.
    - [x] Expor ID selecionado e necessidade do tilemap do logo no dump de debug.
    - [x] Tratar titulo exibido de ROM CGB sem incluir o byte de flag CGB.
    - [x] `--skip-bios` em jogos DMG no modo CGB aplica as paletas selecionadas na palette RAM da PPU.
    - A escolha de paletas depende do header do cartucho, incluindo licenca, checksum do titulo e casos especiais por quarta letra do titulo.

- [ ] Infrared real.
  - Registro existe, mas falta comportamento fisico e transporte externo.

- [ ] Mappers faltantes.
  - Priorizar conforme jogos reais.
  - Possiveis proximos: MBC2, MMM01, HuC1/HuC3, Pocket Camera, rumble nuances.

## Testes

- [x] `cpu_instrs`.
- [x] `instr_timing`.
- [x] `mem_timing`.
- [x] `mem_timing-2`.
- [x] `halt_bug`.
- [ ] `oam_bug` apenas no que afetar CGB real.
- [x] `interrupt_time`.
- [ ] `cgb_sound`.
- [x] Automatizar execucao headless de test ROMs com leitura de serial.
  - [x] `--max-frames` para limitar execucao headless.
  - [x] `--dump-debug-on-exit` para gerar bundle final em execucoes automatizadas.
  - [x] Incluir transcript serial completo no dump de debug.
  - [x] Capturar e avaliar texto serial com status/exit code.
- [x] Gerar screenshot/dump em falhas visuais.

## Documentacao

- [x] `README.md`.
  - Como usar pela UI.
  - Como usar por linha de comando/headless.
  - Como buildar com Maven.
  - Como buildar nativo com GraalVM.
  - Recursos atuais e limitacoes conhecidas.

## Arquitetura De Estado

- Save state salva hardware emulado, nao objetos vivos da aplicacao.
- Cheats/GameShark nao entram no save state; pertencem a configuracao da sessao ou arquivo proprio.
- `AudioSink`, threads, janelas, callbacks e arquivos abertos nunca entram no snapshot.
- Estados devem ser tipados e versionados.
- Componentes com bancos devem expor `MemoryBank` quando isso ajudar debugger e ferramentas.

## Proximas Prioridades Sugeridas

1. Consolidar `cgb_sound` e diferenciar bug de registrador de preferencia/filtro de output.
2. Revisar `cgb_timing` e double speed de forma sistematica.
3. Implementar watchpoints sem custo quando inativos.
4. Implementar gamepad.
5. Evoluir rewind continuo.
6. Implementar link cable local.
