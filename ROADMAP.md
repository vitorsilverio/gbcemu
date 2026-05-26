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
- Menu para reiniciar a ROM atual sem reabrir arquivo, com atalho `F12`.
- Dialogo de abrir ROM lembra a ultima pasta usada.
- Titulo da janela volta para estado neutro ao parar a emulacao.
- Argumentos de linha de comando: `--rom`, `--bios`, `--save-file`, `--headless`, `--skip-bios`, `--no-save`, `--no-bios`.
- Sem BIOS configurada, o emulador usa `--skip-bios` automaticamente.
- Configuracoes persistentes para tela, som, rewind e teclado.
- Remapeamento de teclado.
- Overlay visual para `PLAY`, `PAUSE`, `STOP`, `REW`, `SAVE` e `LOAD`.
- Save states por jogo e por slot (`.sa0`, `.sa1`, `.saN`) com metadata, frame, PC e preview.
- Save RAM e gravado ao parar/reiniciar a emulacao, nao apenas ao fechar a aplicacao.
- Rewind inicial em memoria por snapshots intervalados.
- GameShark com UI para colar listas grandes de codigos.
- Debug separado por area:
  - CPU/disassembly.
  - Memoria e bancos.
  - PPU/tiles/tile maps/paletas.
  - Audio/canais.
  - Cart/MBC.
  - Auto refresh opcional de 1 Hz nas janelas de debug principais.
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
- APU separada em componentes menores com estado explicito e sem conhecer driver/sink/efeitos de audio.
- Gamepad opcional via `input4j`, com configuracao por player, deadzone e mapeamento manual de botoes/eixos.
- Suporte inicial a Super Game Boy visual:
  - deteccao por header SGB;
  - captura de pacotes via `JOYP`;
  - borda oficial enviada pela ROM;
  - composicao de frame 256x224 quando borda esta ativa;
  - dumps graficos SGB para diagnostico.

## Usabilidade

- [x] Configuracoes em abas.
  - [x] Geral.
  - [x] Graficos.
  - [x] Som.
  - [x] Controle.
  - [x] Mover configuracao de BIOS padrao para a janela de configuracoes.
  - [x] Persistir posicao da janela principal e abrir multiplas instancias em cascata.

- [x] ROMs recentes.
  - [x] Mostrar lista dos ultimos jogos abertos no menu `Emulator`.
  - [x] Reabrir rapidamente ROM recente com save/configuracoes usuais.
  - [x] Persistir caminhos recentes e remover entradas inexistentes.
  - [x] Ter acao para limpar historico.

- [ ] Gamepad.
  - [x] Integrar `input4j` de forma opcional em runtime.
  - [x] Detectar controles.
  - [x] Mapear botoes/eixos comuns para Player 1 e Player 2.
  - [x] UI inicial para escolher dispositivo, configurar botoes/eixos e deadzone por player.
  - [x] Persistir perfil por player.
  - [x] Captura automatica de botoes/eixos pressionados.
  - [x] Persistir nome do controle selecionado para sobreviver a mudanca de ordem dos dispositivos.
  - [x] Persistir perfil por controle quando possivel.
  - [ ] Suportar rumble para cartuchos/jogos compativeis, com baixa prioridade por haver poucos jogos.
  - [x] Cartucho expoe suporte/estado de rumble de forma generica para debug e futura integracao com controle.

- [ ] Configuracao de audio mais completa.
  - Latencia/buffer.
  - Device de audio.
  - Perfil de filtro de saida.
  - [x] Aba de som com controles de DSP divertido opcional.
  - [x] DSP presets pos-mixagem: Raw, Warm, Wide, Room, Toy Synth.
  - [x] Controles de intensidade, chorus e reverb.
  - [x] Manter DSP e mute de turbo no driver de audio, fora da logica de hardware da APU.
  - [x] Backend experimental de SoundFont `.sf2` por eventos da APU via Java MIDI.
  - [ ] Refinar mapeamento de instrumentos/programas por canal e presets por jogo.

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

- [ ] Link cable local.
  - Socket local foi abandonado: a primeira versao estavel deve rodar dois emuladores na mesma instancia do GBCEMU.
  - TCP fica para uma futura camada de netplay/lockstep, nao para multiplayer local.
  - Design novo detalhado em [LINK_CABLE_REWRITE.md](LINK_CABLE_REWRITE.md).
  - Estado atual: arquitetura de dois consoles na mesma sessao existe. O foco restante passa a ser usabilidade da sessao local; revisoes profundas de protocolo ficam para quando voltarmos a testar multiplayer de forma dedicada.
  - Arquitetura atual:
    - `Console` representa um Game Boy fisico: CPU, PPU, APU, Bus, Cart, RAM, Serial, controles e estado.
    - `Emulator` representa a sessao/runtime: lista de consoles, loop, throttle, start/stop/pause e coordenacao de link.
  - [x] Remover da UI o fluxo antigo de conexao por socket para evitar uso de algo quebrado.
  - [x] Remover o transporte por socket do caminho runtime padrao do emulador.
  - [x] Extrair o hardware para `Console`.
  - [x] Transformar `Emulator` em sessao com uma lista de consoles.
  - [x] Remover `LinkedEmulationSession`; multiplayer local agora e uma sessao `Emulator` com dois consoles.
  - [x] Expor primeira entrada de menu para iniciar sessao local com duas ROMs.
    - [x] Trocar a execucao inicial por threads independentes por scheduler coordenado/lockstep da sessao.
    - [x] Balancear o scheduler por ciclos de maquina acumulados, nao por uma instrucao inteira por console.
  - [x] Renderizar Player 1 e Player 2 lado a lado na mesma janela.
  - [x] Criar `DirectLinkCable` em memoria conectando diretamente os dois `Serial`.
  - [ ] Revisar `DirectLinkCable` apenas se novos testes mostrarem falha concreta de protocolo.
  - [ ] Validar se a troca de byte deve aguardar o segundo lado armar a transferencia em alguns jogos.
  - [ ] Manter `Serial` como interface do jogo com `SB/SC`, interrupcao serial e shift de bits.
  - [x] Separar controles de Player 1 e Player 2 na sessao local.
  - [x] Implementar configuracoes persistentes de controle separadas para Player 1 e Player 2.
  - [x] Capturar teclado por `KeyEventDispatcher` da aplicacao para input funcionar quando qualquer janela do GBCEMU estiver focada.
  - [x] Bloquear pause individual, rewind, save state e turbo enquanto a sessao link estiver ativa.
  - [x] Expor estado conjunto da sessao link no debug/dump.
  - [x] Ao abrir debug em sessao multi-console, escolher o console alvo por combo.
  - [ ] Evoluir janelas de debug para manter combo interno permanente e trocar a visao sem reabrir janela.
  - [ ] Permitir iniciar/adicionar um segundo console durante a sessao, nao apenas iniciar sempre com dois.
  - [ ] Permitir fechar um console/sessao individual sem interromper tudo.
  - [ ] Permitir destacar a tela de um console para janela/monitor separado.
  - [ ] Validar Tetris primeiro, depois Pokemon.
  - [ ] No futuro, reavaliar TCP como netplay remoto com lockstep explicito.

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
  - [x] Persistir estado do RTC de MBC3 em sidecar `.rtc`, mantendo `.sav` bruto e intercambiavel.

- [ ] Infrared com hardware real.
  - Criar camada de transporte.
  - Integrar Arduino/dispositivo serial.
  - Testar comunicacao com Game Boy Color real.

- [x] Super Game Boy visual.
  - [x] Manter bordas SGB opcionais, ativadas por configuracao de graficos.
  - [x] Detectar cartuchos com flag SGB no header.
  - [x] Capturar pacotes SGB pelo registrador `JOYP`.
  - [x] Implementar retorno basico de `MLT_REQ` para jogos detectarem SGB.
  - [x] Decodificar `CHR_TRN`/`PCT_TRN` para bordas enviadas pelo proprio jogo, usando a janela de transferencia de 5 frames.
  - [x] Renderizar a borda oficial do jogo ao redor do frame 160x144.
  - [x] Salvar estado SGB em save state/rewind quando borda ou comandos SGB estiverem ativos.
  - [x] Exportar PNG da borda e estado SGB no debug bundle.
    - `debug-ppu-sgb-border.png`: borda bruta.
    - `debug-ppu-sgb-frame.png`: frame 160x144 colorizado pelo estado SGB.
    - `debug-ppu-sgb-attributes.png`: mapa de atributos.
    - `debug-ppu-sgb-preview.png`: composicao borda + jogo.
  - [x] Implementar comandos principais de paleta/atributos SGB para colorizacao da area do jogo.
    - `PAL01`/`PAL23`/`PAL03`/`PAL12`.
    - `PAL_SET`.
    - `ATTR_BLK`, `ATTR_LIN`, `ATTR_DIV`, `ATTR_CHR`, `ATTR_SET`.
    - `MASK_EN`, `MLT_REQ`.
    - `PAL_TRN`, `ATTR_TRN`.
  - [x] Corrigir sincronismo de paletas/atributos SGB no jogo.
    - Em Pokemon Red, a borda e a colorizacao da tela central foram validadas apos comparar com a implementacao do mGBA em `referencia/mgba`.
    - Comandos invalidos/idle de joypad nao devem ser aceitos como pacotes SGB reais.
    - Paletas SGB ficam separadas da palette RAM CGB usada por DMG compatibility.
  - [x] Implementar apenas bordas fornecidas pela ROM; sem bordas customizadas de usuario.

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
    - [x] Exibir/exportar borda SGB na janela grafica de PPU quando disponivel.
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
- ROM+RAM, MBC1, MBC2, MBC3 com RTC, MBC5 e RAM externa.
- MBC1 com registrador secundario, modo simple/advanced, ROM banking grande e RAM banking.
- MBC3 limita selecao RAM/RTC a `$00-$07` e `$08-$0C`; valores invalidos nao acessam RAM por acidente.
- Header de cartucho decodifica tamanho de ROM/RAM como unsigned e trata codigos especiais `$52`-`$54`.
- Header de cartucho reporta entry point real de `$0100-$0103`, nao bytes iniciais do logo Nintendo.
- Header de cartucho le exatamente 48 bytes do logo Nintendo em `$0104-$0133`.
- Header de cartucho expoe checksum/global checksum como inteiros e valida header checksum oficial.
- Header de cartucho calcula e valida global checksum ignorando `$014E-$014F`.
- Debug de Cart/MBC expoe tamanho declarado pelo header e validade dos checksums.
- Header de cartucho novo (`old licensee = $33`) exibe titulo sem incluir manufacturer code.

### Faltante / Incerto

- [x] `cgb_sound`.
  - [x] Passar nos testes individuais.
  - [x] Revisar frame sequencer, power on/off da APU, wave channel, DAC e mascaras de leitura.
  - [x] Separar compatibilidade de registradores de qualidade do output para host.
  - Estado atual: sem pendencia audivel conhecida depois da correcao de timing. Manter como area de regressao/testes, nao como prioridade ativa.

- [ ] Timing CGB.
  - Estado atual: nenhum bug especifico aberto, mas ainda falta uma revisao dirigida para fechar o assunto.
  - Validar double speed em CPU, timer, serial, PPU, DMA, HDMA e APU contra referencias/test ROMs quando tivermos suite apropriada.
  - Garantir que componentes que nao dobram no CGB continuem no clock correto.
  - Documentar quais subsistemas usam clock de CPU, dot clock, machine cycle ou frame sequencer para evitar regressao futura.

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
    - [x] Prioridade CGB por ordem de OAM quando `OPRI=0`.
    - [x] Modo compatibilidade DMG por coordenada X/ordem de OAM quando `OPRI=1`.
    - [x] Atributo de prioridade OBJ respeita `LCDC.0` em CGB.
  - [x] Window usa contador interno de linha e so avanca quando a Window realmente inicia na scanline.
  - [x] Condicao `WY == LY` da Window fica latched no frame mesmo se `WY` mudar depois.
  - [x] `LCDC.0` no CGB desliga prioridade BG/window contra sprites, mas nao apaga o BG.
  - [x] Busca OAM em CGB coleta candidatos mesmo com `LCDC.1=0`; mistura final ainda respeita `LCDC.1`.
  - Window edge cases restantes.
    - `WX=0` com `SCX&7>0` encurta mode 3 em 1 dot.
    - [x] `WX=166` permite disparo no fim da scanline; `WX=167` nao inicia Window.
    - Alteracoes mid-scanline de `WX`, `WY` e `LCDC.5` ainda precisam de validacao visual.
  - Penalidades de fetch e impacto de sprites/window.
    - Penalidade inicial por `SCX&7`.
    - Penalidades por sprites encontrados no fetcher.
    - Reinicio do fetcher quando Window inicia.
  - [x] Nao implementar corrupcao do `oam_bug`: Pan Docs documenta que CGB/AGB nao sao afetados, inclusive rodando software DMG.
  - [x] Manter apenas bloqueios CGB reais de OAM durante mode 2/3.

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
  - Registro do CGB existe, mas falta comportamento fisico e transporte externo.
  - HuC1 ja expoe modo IR basico do cartucho, lendo `$C0` quando nao ha luz externa simulada.
  - HuC3 ja expoe modo IR basico do cartucho, tambem sem transporte fisico.

- [ ] Mappers faltantes.
  - Priorizar conforme jogos reais.
  - [x] MBC2 com ROM banking, RAM interna 512 x 4-bit e save bruto de 512 bytes.
  - [x] HuC1 inicial com ROM/RAM banking e modo IR basico.
  - [x] HuC3 inicial com ROM/RAM banking, selecao de modos e mailbox RTC minimo.
  - [x] HuC3 persiste mailbox/registradores RTC internos em sidecar `.huc3rtc`, mantendo `.sav` bruto.
  - [x] MMM01 inicial com modo unmapped mapeando o menu nos ultimos 32 KiB e entrada em modo mapped estilo MBC1.
  - [x] MMM01 aplica mascaras basicas de ROM/RAM para bits reservados pela selecao de jogo.
  - Possiveis proximos: MBC1M, MMM01 multiplex completo, Pocket Camera, rumble nuances.
  - HuC3 ainda precisa RTC completo e speaker.

## Testes

- [x] `cpu_instrs`.
- [x] `instr_timing`.
- [x] `mem_timing`.
- [x] `mem_timing-2`.
- [x] `halt_bug`.
- [x] `oam_bug` ignorado como bug DMG-only; CGB real nao sofre a corrupcao testada por essa suite.
- [x] `interrupt_time`.
- [x] `cgb_sound`.
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

## Pendencias Atuais

### Mais Importantes Agora

1. Revisar timing CGB/double speed de forma sistematica.
   - Nao ha bug especifico aberto; e uma auditoria para garantir que CPU/timer/serial/PPU/DMA/APU estao no clock certo em normal/double speed.
2. Evoluir perfis de controle.
   - Perfil por controle.
   - Rumble fica como baixa prioridade.
3. Melhorar usabilidade do link local.
   - Separar/destacar janelas.
   - Adicionar um segundo console depois que a sessao ja iniciou.
   - Fechar uma sessao/console individual sem derrubar tudo.
4. Escolher proxima tarefa de compatibilidade baseada em jogo/teste real.

### Menores / Depois

- Turbo acima de 3.5x com menos custo de renderizacao/audio.
- APU/fidelidade sonora: monitorar regressao, mas sem pendencia audivel conhecida no momento.
- HuC3 RTC completo e speaker.
- Pocket Camera e mappers raros conforme necessidade de jogos reais.
- GraalVM native-image no Windows com metadata AWT/Swing estavel.
- Infrared fisico via dispositivo externo.
