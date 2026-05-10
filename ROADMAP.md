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

- [ ] Gamepad.
  - Detectar controles.
  - Mapear botoes.
  - Persistir perfil por controle quando possivel.

- [ ] Configuracao de audio mais completa.
  - Latencia/buffer.
  - Device de audio.
  - Perfil de filtro de saida.

- [ ] Rewind continuo.
  - Restaurar enquanto o usuario segura uma tecla/botao.
  - Evitar custo quando rewind estiver desativado.
  - Avaliar snapshots menores ou delta compression.

- [ ] Link cable.
  - Suportar TCP sockets para conexao via rede.
  - Suportar Unix domain sockets para conexao local simples, sem bloqueio de firewall.
  - UI para conectar/desconectar.
  - Sincronizacao suficiente para trocas e batalhas.

- [ ] Infrared com hardware real.
  - Criar camada de transporte.
  - Integrar Arduino/dispositivo serial.
  - Testar comunicacao com Game Boy Color real.

## Debug

- [x] Watchpoints.
  - [x] Breakpoint por leitura/escrita de endereco.
  - [x] Breakpoint por valor simples opcional.
  - [x] Desenhar para custo zero quando nenhum watchpoint estiver ativo.

- [ ] Step/debug controls.
  - Step instruction.
  - Step frame.
  - Step scanline.
  - Run until VBlank/HBlank.
  - Exige arquitetura leve para nao repetir a lentidao da tentativa inicial.

- [ ] Memory editor seguro.
  - Nao usar apenas `bus.write` como padrao, porque dispara efeitos colaterais e falha em ROM/areas bloqueadas.
  - Modo `hardware write`: via bus, explicitamente com efeitos colaterais.
  - Modo `raw bank edit`: via `MemoryBank.writeBank`, limitado a memorias editaveis como WRAM, VRAM, HRAM e cart RAM.
  - Permitir inicialmente apenas quando pausado.

- [ ] Export de debug para Codex.
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

- [ ] Disassembler avancado.
  - Decodificar operandos e destinos de jumps/calls de forma mais rica.
  - Invalidar cache quando memoria executavel ou banco relevante mudar.

## Compatibilidade CGB

### Ja Existe Base

- KEY0/KEY1 e troca de velocidade via `STOP`.
- Timer e serial com consideracao inicial de double speed.
- VRAM bank (`VBK`) e WRAM bank (`SVBK`).
- CGB palettes (`BGPI/BGPD`, `OBPI/OBPD`).
- Atributos CGB de tile map: banco, paleta, flip e prioridade.
- OAM com atributos CGB: banco, paleta, flip e prioridade.
- HDMA/GDMA inicial.
- Infrared register (`FF56`) inicial.
- Object priority mode (`OPRI`).
- PCM registers (`FF76/FF77`) para saida digital da APU.
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

- [ ] HDMA/GDMA completo.
  - Confirmar bloqueios de bus e timing por bloco.
  - Confirmar comportamento de cancelamento de HBlank HDMA.
  - Validar origem/destino, mascaras e leitura de `HDMA5`.

- [ ] Bloqueios de acesso CGB.
  - VRAM durante mode 3.
  - OAM durante mode 2/3.
  - Paletas durante modos bloqueados.
  - Wave RAM durante CH3 ativo.

- [ ] PPU CGB edge cases.
  - Prioridade BG/window/sprite em CGB e modo compatibilidade DMG.
  - Window edge cases (`WX`, `WY`, reinicio por linha).
  - Penalidades de fetch e impacto de sprites/window.
  - OAM bug apenas se afetar CGB real ou jogos CGB.

- [ ] Paletas e boot behavior.
  - Confirmar estado pos-BIOS.
  - Confirmar mapeamento RGB555/BGR555 e conversao para RGB host.
  - Confirmar comportamento de jogos DMG rodando em modo CGB.

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
- [ ] `halt_bug`.
- [ ] `oam_bug` apenas no que afetar CGB real.
- [ ] `interrupt_time`.
- [ ] `cgb_timing`.
- [ ] `cgb_sound`.
- [ ] Automatizar execucao headless de test ROMs com leitura de serial.
- [ ] Gerar screenshot/dump em falhas visuais.

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
