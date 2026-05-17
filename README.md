# GBC EMU

Emulador de Game Boy Color em Java, com foco em compatibilidade CGB, ferramentas de debug integradas e uso pratico fora da IDE.

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
- `Emulator > Settings...`: abre configuracoes em abas de geral, graficos, som e controles.
- `Emulator > Pause`, `Resume`, `Stop`: controla a execucao.
- `Emulator > Save states`: salva/carrega slots por jogo e gerencia estados.
- `Emulator > Cheats`: abre a janela de GameShark.
- `Emulator > Multiplayer`: configura link cable experimental.
- `Debug`: abre janelas separadas de CPU, memoria, PPU, audio, cart/MBC e dumps.

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
- MBC1, MBC3 com RTC, MBC5 e ROM-only.
- RAM externa e save `.sav`.
- Save states por jogo e slots `.sa0`, `.sa1`, `.saN`, com metadata e preview.
- Rewind por snapshots.
- GameShark.
- Filtros de tela, incluindo xBRZ.
- Turbo configuravel por tecla segurada ou toggle, com audio silenciado e frameskip automatico durante a aceleracao.
- Debug separado por area:
  - CPU/disassembly e breakpoints.
  - Memoria, bancos e edicao segura.
  - PPU, tiles, tile maps e paletas.
  - Audio por canal.
  - Cart/MBC.
- Dumps de debug para investigacao externa.
- Multiplayer/link cable experimental.

## Limitacoes Conhecidas

- `cgb_sound`, `cgb_timing`, `halt_bug`, `interrupt_time` e partes de `oam_bug` ainda precisam ser corrigidos.
- Link cable ainda tem problemas de negociacao master/slave.
- Gamepad ainda nao foi integrado.
- Rumble de cartuchos compativeis ainda precisa ser ligado a um backend de controle.
- Build nativo com GraalVM no Windows ainda pode exigir ajustes de metadata AWT/Swing.
- A fidelidade do audio ainda esta em evolucao.

## Roadmap

As metas atuais ficam em [ROADMAP.md](ROADMAP.md).
