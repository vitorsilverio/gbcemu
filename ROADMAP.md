# GBC EMU Roadmap

Este documento organiza as melhorias e metas do emulador. A ideia é manter uma lista viva, marcar progresso aos poucos e evitar perder contexto entre sessões de debug.

## Objetivos Gerais

- Priorizar Game Boy Color, sem complicar demais com detalhes que so importam para DMG quando isso atrapalhar a evolucao principal.
- Melhorar a usabilidade para testar jogos reais sem depender do IntelliJ.
- Criar ferramentas internas de debug que sejam uteis tanto para uso manual quanto para analise via Codex.
- Aumentar compatibilidade gradualmente, validando com test ROMs e jogos reais.

## Funcionalidades De Usabilidade

- [ ] Unificar a experiencia em uma unica janela.
  - O emulador nao deve abrir uma janela separada apenas para iniciar ROM.
  - `Open ROM` nao deve relancar a aplicacao inteira.
  - O ideal e permitir carregar/trocar ROM recriando o runtime internamente.

- [ ] Save state.
  - Capturar estado completo de CPU, bus, RAM, VRAM, OAM, IO, PPU, Timer, APU, cart/MBC e periféricos.
  - Salvar/carregar snapshots manualmente.
  - Garantir que snapshots nao compartilhem referencias vivas com o estado em execucao.

- [ ] Rewind.
  - Guardar os ultimos 15 segundos por padrao.
  - Tornar a duracao configuravel.
  - Usar ring buffer de snapshots por frame ou intervalos fixos.
  - Restaurar snapshots enquanto o usuario segura uma tecla/botao.

- [ ] GameShark / cheats.
  - Suportar codigos GameShark/Game Genie relevantes para GB/GBC.
  - UI para adicionar, remover, ativar e desativar cheats.
  - Aplicar patches de memoria de forma rastreavel para debug.

- [ ] Serial com link cable.
  - Implementar cabo serial entre duas instancias do emulador.
  - Avaliar backend por socket local.
  - Criar UI para conectar/desconectar.

- [ ] Infrared com hardware real.
  - Conectar com dispositivo real de infrared via Arduino.
  - Permitir comunicacao com um Game Boy Color real.
  - Criar uma camada de transporte para manter o emulador independente do hardware especifico.

- [ ] Menu de configuracoes.
  - Som: volume, mute, latencia/buffer, device.
  - Controle: teclado, gamepad, remapeamento.
  - Tela: escala, filtros, aspect ratio, cores, fullscreen.

- [ ] Suporte a controle.
  - Detectar gamepads.
  - Mapear botoes.
  - Persistir configuracao.

## Funcionalidades De Debug

- [x] Janela inicial de debugger.
  - CPU/PPU snapshot.
  - Lista de instrucoes ao redor do PC.
  - Runtime memory map por componente.
  - Tiles, tile maps e paletas.
  - Dump para `target/debug-*`.

- [x] Visualizador de memoria.
  - Tabela com 16 bytes por linha.
  - Selecionar regioes comuns: ROM, VRAM, WRAM, OAM, IO, HRAM.
  - Dump textual para `target/debug-memory.txt`.

- [ ] Breakpoints.
  - [x] Breakpoint por PC.
  - Breakpoint por leitura/escrita de endereco.
  - Breakpoint por valor/condicao simples.
  - Integrar com pause/resume/step.

- [ ] Step/debug controls.
  - Step instruction.
  - Step frame.
  - Step scanline.
  - Run until VBlank/HBlank.

- [ ] Edicao de memoria.
  - Permitir editar celulas da tabela de memoria.
  - Escrever via `bus.write`.
  - Inicialmente permitir edicao apenas quando pausado.
  - Indicar regioes bloqueadas ou com efeitos colaterais.

- [ ] Melhorar disassembler.
  - [x] Substituir texto puro por tabela na UI.
  - [x] Destacar a instrucao atual do PC.
  - [x] Mostrar nomes reais das familias principais e opcodes CB.
  - Decodificar operandos corretamente.
  - Mostrar bytes e destino de jumps/calls.

- [ ] Debug snapshots para Codex.
  - Exportar estado em JSON alem de TXT/PNG.
  - Incluir registradores, memoria selecionada, PPU, cart/MBC, timer e interrupcoes.

- [ ] Debug de audio.
  - Mostrar estado dos canais 1, 2, 3 e 4.
  - Permitir alterar volume/mute individual dos canais em tempo de execucao para testes.
  - Essas alteracoes sao ferramentas de debug e nao devem entrar no save state.

## Funcionalidades De Compatibilidade

- [ ] Implementar o resto dos memory mappers.
  - Revisar suporte atual: ROM only, MBC1, MBC3, MBC5.
  - Implementar mappers faltantes conforme prioridade de jogos reais.
  - Validar RAM externa, rumble, RTC e bancos grandes.

- [ ] Passar no teste de som do CGB.
  - Continuar a partir dos testes individuais do `cgb_sound`.
  - Corrigir comportamento de frame sequencer, wave channel e detalhes de power.
  - Evitar mexer em audio output/buffer enquanto a meta for compatibilidade de registradores.

- [ ] Completar funcionalidades faltantes do CGB.
  - Revisar HDMA/GDMA.
  - Revisar double speed e efeitos no timer/serial/APU.
  - Revisar VRAM bank, WRAM bank e registradores CGB.
  - Revisar prioridades de BG/window/sprites.
  - Revisar comportamento de paletas durante modos bloqueados.

- [ ] Melhorar testes automatizados.
  - Manter test ROMs conhecidas como diagnostico.
  - Separar testes headless rapidos de testes visuais longos.
  - Automatizar leitura de serial quando disponivel.
  - Gerar screenshots de falha para PPU/debug.

## Ordem Sugerida

1. Consolidar a janela unica e runtime recarregavel.
2. Adicionar pause/resume/stop mais robustos e step instruction.
3. Implementar breakpoints simples por PC.
4. Permitir edicao de memoria quando pausado.
5. Implementar save state basico.
6. Evoluir save state para rewind dos ultimos 15 segundos.
7. Retomar compatibilidade CGB: `cgb_sound`, CGB timing e funcionalidades faltantes.
8. Expandir usabilidade: configuracoes, controle, cheats e link cable.

## Notas De Implementacao

- Save state e rewind devem ser tratados como infraestrutura central, nao como recurso superficial de UI.
- Breakpoints e edicao de memoria devem funcionar sem deixar o emulador lento quando o debugger estiver fechado.
- Dumps do debugger devem continuar sendo legiveis por humanos e por ferramentas externas.
- O emulador deve continuar utilizavel mesmo quando nenhuma ROM estiver carregada.
