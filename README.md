# GBC EMU

Emulador de Game Boy Color em Java, com foco em compatibilidade CGB, usabilidade fora da IDE e ferramentas de debug integradas.

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
- `Emulator > Recent ROMs`: reabre rapidamente os ultimos jogos usados e permite limpar o historico.
- `Emulator > Start linked session...`: abre duas ROMs lado a lado em uma sessao local de link cable em memoria. Esta area ainda e experimental.
- `Emulator > Add Console 2...`: adiciona um segundo jogo a sessao atual sem reiniciar o primeiro console.
  - `Emulator > Add Console 2 from Recent ROMs` faz o mesmo usando o historico de ROMs recentes.
  - Player 1 usa o mapeamento configurado.
  - Player 2 tem mapeamento proprio em `Settings > Controls`.
  - Gamepads detectados pelo `input4j` tambem entram no controle composto: primeiro controle para Player 1, segundo controle para Player 2.
  - Em `Settings > Controls`, cada player tem uma sub-aba propria para teclado, dispositivo, deadzone e nomes de botoes/eixos do gamepad.
  - No mapeamento de gamepad, use `Capture` para detectar botoes/eixos automaticamente, ou edite nomes separados por virgula; eixos aceitam `+`/`-`, como `AXIS_Y-` para cima.
  - O botao `Components...` mostra os nomes/valores atuais reportados pelo controle selecionado.
  - O controle escolhido tambem e lembrado pelo nome reportado pelo sistema, e mapeamento/deadzone ficam salvos como perfil desse controle.
  - Cartuchos com rumble acionam a vibracao do gamepad selecionado quando o backend `input4j` e o dispositivo suportarem esse recurso.
  - O teclado e capturado pela aplicacao enquanto uma janela do GBCEMU estiver focada, exceto durante edicao de campos de texto.
  - Internamente, cada jogo roda como um `Console` dentro da mesma sessao `Emulator`, que coordena o tick dos dois.
- `Emulator > Display`: abre uma janela destacada para o Console 1 ou Console 2, util para colocar cada tela em um monitor sem separar a sessao.
- `Emulator > Stop Console`: interrompe apenas um console ativo. Se o Console 1 for fechado enquanto o Console 2 continua, a tela restante e promovida na janela principal.
  - Quando a sessao volta a ter apenas um console, restart e save states voltam a usar a ROM restante.
- `Emulator > Settings...`: abre configuracoes em abas de geral, graficos, som e controles.
  - A aba de graficos permite ativar bordas Super Game Boy enviadas pela propria ROM quando o cartucho suportar SGB.
  - A aba de som permite escolher device de saida, buffer em milissegundos e presets DSP opcionais (`Raw`, `Warm`, `Wide`, `Room`, `Toy Synth`) com intensidade, chorus e reverb.
  - Tambem ha suporte experimental a SoundFont `.sf2` via Java MIDI, com modos de overlay/substituicao.
- `Emulator > Pause`, `Resume`, `Stop`, `Restart`: controla a execucao.
- `Emulator > Save states`: salva/carrega slots por jogo e gerencia estados.
- `Emulator > Cheats`: abre a janela de GameShark.
- `Debug`: abre janelas separadas de CPU, memoria, PPU, audio, cart/MBC e dumps. Em sessao local de link, as janelas de debug permitem trocar o console dentro da propria janela e atualizam a lista quando consoles sao adicionados ou parados. O dump gera um indice da sessao com dumps separados por console e um resumo `clockTiming` para investigar double speed/timing CGB. Os arquivos de debug ficam em `debug/`, que e ignorado pelo Git. O debug de PPU tambem exporta imagens auxiliares de SGB quando disponiveis.

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

## Frontend Android

O suporte a APK esta em primeira versao jogavel no modulo `android-app`, feito em Java com Android Gradle Plugin. Ele abre uma Activity fullscreen, importa ROMs pelo seletor do Android para armazenamento privado do app, lembra a ultima ROM, cria um `Console`/`Emulator` real do core, renderiza a PPU em `SurfaceView`, envia samples para `AudioTrack`, usa controles virtuais na tela e mapeia botoes fisicos Android basicos.

A PPU/SGB ja expoem frames e imagens de debug como `RawImage`, sem `BufferedImage`/AWT no core. A conversao para Swing/PNG fica no frontend desktop. O driver Java Sound/MIDI/DSP tambem fica no frontend desktop (`gui.audio`), separado da APU.

Build Android debug, quando o Android SDK/Gradle estiverem configurados:

```powershell
gradle :android-app:assembleDebug
```

Se o Gradle nao encontrar o Android SDK, crie um `local.properties` na raiz do projeto. Exemplo:

```properties
sdk.dir=C:/Users/user/AppData/Local/Android/Sdk
```

Tambem e possivel definir `ANDROID_HOME` apontando para a mesma pasta. O arquivo `local.properties` e local da maquina e fica ignorado pelo Git; use [local.properties.example](local.properties.example) como referencia.

Se o build falhar dentro de `JdkImageTransform`/`jlink`, especialmente usando GraalVM como JDK do Gradle, o projeto ja fixa `org.gradle.java.home=C:/Users/user/.jdks/ms-25.0.2` e deixa `android.disableJdkImageTransform=true` em [gradle.properties](gradle.properties). Ajuste esse caminho se trocar o JDK usado pelo Gradle Android.

Tambem existe um perfil Maven de conveniencia que delega para o Gradle Android:

```powershell
mvn -Pandroid-apk package
```

O Maven continua sendo o build principal do desktop/core. O APK ainda usa o source set Java principal filtrando `Main`, `gui` e o transporte multiplayer por socket; separar em `gbcemu-core`, `gbcemu-desktop` e `gbcemu-android` continua sendo a proxima evolucao de arquitetura.

O plano de separacao esta em [ANDROID_FRONTEND_PLAN.md](ANDROID_FRONTEND_PLAN.md).

## Recursos Atuais

- Emulacao inicial de Game Boy Color com BIOS CGB.
- Skip-bios automatico quando nenhuma BIOS padrao esta configurada.
- MBC1, MBC2, MBC3 com RTC, MBC5, HuC1, HuC3, MMM01 e ROM-only.
- RAM externa e save `.sav` bruto/intercambiavel com outros emuladores.
- Save states por jogo e slots `.sa0`, `.sa1`, `.saN`, com metadata e preview.
- Rewind por snapshots.
- GameShark.
- Historico de ROMs recentes no menu.
- Filtros de tela, incluindo xBRZ.
- Bordas Super Game Boy opcionais para jogos que enviam borda propria, com colorizacao SGB da area do jogo.
- Filtros DSP opcionais de audio pos-mixagem para brincar com o som sem alterar a APU.
- SoundFont experimental para tocar os canais da APU como instrumentos MIDI carregados de um `.sf2`.
- Turbo configuravel por tecla segurada ou toggle, com audio silenciado e frameskip automatico durante a aceleracao.
- APK Android inicial com seletor de ROM, tela, controles virtuais, AudioTrack e saves basicos.
- Debug separado por area:
  - CPU/disassembly e breakpoints.
  - Memoria, bancos e edicao segura.
  - PPU, tiles, tile maps, paletas e imagens SGB auxiliares.
  - Audio por canal.
  - Cart/MBC.
- Dumps de debug para investigacao externa em `debug/`.
- Overlays visuais para pause, resume, stop, rewind, save e load.

## Limitações Conhecidas

- Timing CGB/double speed ainda precisa de validacao sistematica.
- A corrupcao testada por `oam_bug` e DMG-only; o foco atual e manter os bloqueios CGB reais de OAM/VRAM.
- Link cable local ainda e experimental: a sessao usa cabo em memoria e pode abrir/adicionar um segundo console durante a execucao. Telas podem ser abertas em janelas destacadas e consoles podem ser fechados individualmente. O protocolo ainda deve ser revisitado com jogos reais especificos.
- Gamepad tem integracao opcional por `input4j`; suporte a rumble depende do driver/dispositivo reportar vibracao pelo backend.
- Build nativo com GraalVM no Windows ainda pode exigir ajustes de metadata AWT/Swing.
- Os filtros DSP e SoundFont sao opcionais/experimentais; a APU base deve continuar priorizando comportamento de hardware.
- O frontend Android ainda e inicial: sem UI de configuracoes, save states, rewind, SGB composto, recentes completos ou perfis de controles fisicos.

## Roadmap

As metas atuais ficam em [ROADMAP.md](ROADMAP.md).

## Como contribuir

Issues e pull requests são bem-vindos — ver [CONTRIBUTING.md](CONTRIBUTING.md).

## Autor e contato

Feito por [Vitor Silvério Rodrigues](https://vitorsilverio.dev/) — blog/currículo com mais
detalhes sobre este e outros projetos. Contato: vitor.silverio.rodrigues@gmail.com ou uma
[issue](https://github.com/vitorsilverio/gbcemu/issues) neste repositório.
