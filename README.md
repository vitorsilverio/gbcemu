# GBC EMU

Emulador de Game Boy Color em Java, com foco em compatibilidade CGB, ferramentas de debug integradas.

## Requisitos

- Java 21 ou superior.
- Maven 3.9 ou superior.
- Opcional: GraalVM com `native-image` para build nativo.

## Executando Pela UI

```powershell
mvn compile exec:java -Dexec.mainClass=dev.vitorsilverio.gbcemu.Main
```

Ao abrir sem argumentos, o emulador mostra a janela principal sem exigir ROM imediatamente.

Menus principais:

- `Emulator > Start ROM...`: abre uma ROM `.gb` ou `.gbc`.
- `Emulator > Start linked session...`: abre duas ROMs lado a lado em uma sessao local de link cable em memoria.
  - Player 1 usa o mapeamento configurado.
  - Player 2 tem mapeamento proprio em `Settings > Controls`.
  - Gamepads detectados pelo `input4j` tambem entram no controle composto: primeiro controle para Player 1, segundo controle para Player 2.
  - Em `Settings > Controls`, cada player pode escolher o dispositivo, deadzone e nomes de botoes/eixos do gamepad.
  - No mapeamento de gamepad, use nomes separados por virgula; eixos aceitam `+`/`-`, como `AXIS_Y-` para cima.
  - O botao `Components...` mostra os nomes/valores atuais reportados pelo controle selecionado.
  - Internamente, cada jogo roda como um `Console` dentro da mesma sessao `Emulator`, que coordena o tick dos dois.
- `Emulator > Settings...`: abre configuracoes em abas de geral, graficos, som e controles.
  - A aba de som inclui presets DSP opcionais (`Raw`, `Warm`, `Wide`, `Room`, `Toy Synth`) com intensidade, chorus e reverb.
  - Tambem ha suporte experimental a SoundFont `.sf2` via Java MIDI, com modos de overlay/substituicao.
- `Emulator > Pause`, `Resume`, `Stop`, `Restart`: controla a execucao.
- `Emulator > Save states`: salva/carrega slots por jogo e gerencia estados.
- `Emulator > Cheats`: abre a janela de GameShark.
- `Debug`: abre janelas separadas de CPU, memoria, PPU, audio, cart/MBC e dumps. Em sessao local de link, o debug pergunta qual console visualizar e o dump gera um indice da sessao com dumps separados por console.

## Linha De Comando

```powershell
mvn compile exec:java -Dexec.mainClass=dev.vitorsilverio.gbcemu.Main -Dexec.args="--rom .\silver.gbc"
```

Opcoes disponiveis:

```text
--rom <path>              ROM para carregar.
--bios <path>             BIOS para carregar. Padrao: BIOS configurada na UI.
--no-bios                 Nao carrega BIOS; implica --skip-bios.
--save-file <path>        Arquivo .sav. Padrao: mesmo nome da ROM com extensao .sav.
--no-save                 Desativa persistencia de save.
--skip-bios               Inicia direto em 0x0100.
--headless                Executa sem janela e sem audio, sem throttle.
--max-frames <n>          Para automaticamente apos n frames renderizados.
--dump-debug-on-exit      Gera bundle de debug ao parar.
--expect-serial <text>    Retorna codigo 2 se o serial nao contiver o texto.
--fail-serial <text>      Retorna codigo 3 se o serial contiver o texto.
--help                    Mostra ajuda.
```

Exemplo para ROMs de teste:

```powershell
mvn compile exec:java -Dexec.mainClass=dev.vitorsilverio.gbcemu.Main -Dexec.args="--rom .\test-roms\cpu_instrs.gb --headless --max-frames 6000 --expect-serial Passed --dump-debug-on-exit"
```

## Build Com Maven

```powershell
mvn test
mvn package
```

O artefato Java fica em `target/`.

## Build Nativo Com GraalVM

```powershell
mvn -Pnative package
```

Observacoes:

- A UI usa Swing/AWT, entao o build nativo precisa incluir `java.desktop`.
- Se trocar a versao do GraalVM/JDK, limpe `target/` antes de testar novamente.
- O suporte nativo no Windows ainda deve ser tratado como experimental.

## Recursos Atuais

- Emulacao inicial de Game Boy Color com BIOS CGB.
- Skip-bios automatico quando nenhuma BIOS padrao esta configurada.
- MBC1, MBC2, MBC3 com RTC, MBC5, HuC1, HuC3, MMM01 e ROM-only.
- RAM externa e save `.sav` bruto/intercambiavel com outros emuladores.
- Save states por jogo e slots `.sa0`, `.sa1`, `.saN`, com metadata e preview.
- Rewind por snapshots.
- GameShark.
- Filtros de tela, incluindo xBRZ.
- Filtros DSP opcionais de audio pos-mixagem para brincar com o som sem alterar a APU.
- SoundFont experimental para tocar os canais da APU como instrumentos MIDI carregados de um `.sf2`.
- Turbo configuravel por tecla segurada ou toggle, com audio silenciado e frameskip automatico durante a aceleracao.
- Debug separado por area:
  - CPU/disassembly e breakpoints.
  - Memoria, bancos e edicao segura.
  - PPU, tiles, tile maps e paletas.
  - Audio por canal.
  - Cart/MBC.
- Dumps de debug para investigacao externa.
- Overlays visuais para pause, resume, stop, rewind, save e load.

## Limitações Conhecidas

- cgb timing ainda precisa ser corrigido.
- A corrupcao testada por `oam_bug` e DMG-only; o foco atual e manter os bloqueios CGB reais de OAM/VRAM.
- Link cable local ainda e experimental: a sessao abre dois emuladores lado a lado na mesma instancia e usa cabo em memoria. TCP/netplay fica para o futuro.
- Gamepad tem integracao inicial opcional por `input4j`; a captura automatica de botoes/eixos e perfis por controle ainda ficam para evolucao.
- Rumble de cartuchos compativeis ainda precisa ser ligado a um backend de controle.
- Build nativo com GraalVM no Windows ainda pode exigir ajustes de metadata AWT/Swing.
- A fidelidade do audio ainda esta em evolucao.

## Roadmap

As metas atuais ficam em [ROADMAP.md](ROADMAP.md).
