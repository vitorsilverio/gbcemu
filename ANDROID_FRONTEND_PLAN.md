# Android Frontend Plan

Objetivo: preparar o GBCEMU para ter frontends diferentes sem contaminar o nucleo de emulacao com detalhes de Swing, Java Sound, input4j ou Android.

## Camadas Desejadas

### Core Portavel

O core deve conter apenas hardware emulado e estado:

- CPU, PPU, APU, timer, interrupcoes, DMA/HDMA, serial, joypad, IR e registradores CGB.
- Cartuchos, MBCs, RAM externa, RTC, saves e save states.
- Link cable como conexao entre consoles, sem depender de sockets ou UI.
- Debug snapshots/dumps em estruturas neutras.

Regras:

- Nao importar `java.awt`, `javax.swing`, `javax.sound`, `input4j` ou Android SDK.
- Receber input como estado de botoes, nao como eventos de teclado/gamepad.
- Emitir frames e samples por interfaces pequenas.
- Rumble deve sair por uma interface de host, sem cartucho conhecer o dispositivo.

### Frontend Desktop

O frontend desktop fica responsavel por:

- Swing/AWT: janela principal, menus, dialogs, janelas de debug e renderizacao.
- Java Sound: device, buffer, DSP opcional, SoundFont e sink de audio.
- input4j: gamepads, captura de botoes/eixos e rumble.
- File dialogs e persistencia de configuracoes no formato atual.

### Frontend Android

O frontend Android futuro fica responsavel por:

- Activity/lifecycle: pause/resume/stop sem corromper saves.
- SurfaceView ou TextureView para renderizar frames.
- AudioTrack/AAudio para samples da APU.
- Controles na tela com layout configuravel.
- Android gamepad API para controles fisicos.
- Vibrator/GameController rumble para cartuchos compativeis.
- Scoped storage para ROMs, saves, save states e configuracoes.

## Interfaces A Extrair

Ordem sugerida para reduzir risco:

1. `FrameSink`: recebe frame do PPU/SGB em tamanho nativo, sem conhecer Swing.
2. `AudioSampleSink`: recebe samples finais da APU; desktop aplica DSP e device depois.
3. `InputProvider`: fornece estado atual de botoes por console/player.
4. `RumbleOutput`: recebe intensidade/eventos de rumble.
5. `StorageProvider`: resolve ROM, `.sav`, `.rtc`, save states e configuracoes.
6. `ConsoleLifecycle`: start/stop/pause/restart por console sem acoplar menu.

## Estado Atual Do Codigo

Hoje `Console` ainda cria ou conhece objetos de frontend desktop:

- `KeyboardController`
- `GamepadController`
- `AudioOutput`

Primeira separacao feita:

- `ConsoleDisplay` fica no core como porta inicial de tela.
- `SwingConsoleDisplay` adapta `EmulatorWindow` para desktop.
- `ConsoleInput` fica no core como porta de botoes, turbo e rumble.
- `DesktopConsoleInput` compoe `KeyboardController`, `GamepadController`, input4j e rumble para desktop.
- `ConsoleAudioOutput` fica no core como porta de samples/configuracao de audio.
- `AudioOutput` virou adaptador desktop de saida, device, buffer, DSP e SoundFont.
- `Console` nao abre mais janelas de debug/cheats diretamente.
- `Console` nao cria mais `EmulatorWindow`, `KeyboardController`, `GamepadController` ou `AudioOutput`.
- `Console` nao importa AWT/ImageIO diretamente para dumps PNG; isso ficou isolado em utilitario de debug desktop/JVM.
- PPU e Super Game Boy nao expõem mais `BufferedImage`; imagens de frame/debug usam `RawImage`, e o desktop converte para `BufferedImage` em `SwingImages`.
- Dumps PNG de debug saem por `DebugImageSink`; o core pode gerar apenas JSON, enquanto o desktop injeta `PngDebugImageSink` quando precisa das imagens.
- `KeyboardController`, `GamepadController` e `CpuDebugWindow` foram movidos para o pacote `gui`; os pacotes `controller` e `debug` ficam restritos a contratos/dados neutros.
- `AudioOutput`, `AudioSinkFactory`, `SourceDataLineSink`, `SoundFontSynth` e o DSP de host foram movidos para `gui.audio`; o pacote `audio` fica com a APU e `AudioSampleOutput`.
- `Emulator` nao abre mais janelas de debug/cheats e nao cria adaptadores desktop; ele coordena `Console` e expoe `DebugTarget` neutro.
- O desktop monta `ConsoleDisplay`, `ConsoleInput` e `ConsoleAudioOutput` antes de entregar consoles ao runtime.
- `AppSettings` nao depende mais de `java.awt.event.KeyEvent` para defaults de teclado.
- `android-app` existe como scaffold Java com Activity fullscreen, `SurfaceView`, controles virtuais, botoes fisicos Android basicos, `AudioTrack` e rumble via `Vibrator`.
- O Android ja importa ROMs pelo seletor do Android para armazenamento privado do app e lembra a ultima ROM escolhida.
- O Android agora cria um `Console`/`Emulator` real usando adaptadores proprios: `GbcEmulatorSurface`, `AndroidInputState` e `AndroidAudioOutput`.
- O modulo Android ainda consome o source set Java principal filtrando desktop (`Main`, `gui`) em vez de depender de um modulo `gbcemu-core` separado.

A refatoracao deve fazer `Console` representar somente o Game Boy fisico emulado. A sessao `Emulator` coordena um ou mais consoles, e cada frontend conecta displays, audio e input por adaptadores.

## Build APK

O APK ja existe como primeira versao jogavel. O caminho preferido de arquitetura ainda e:

1. Criar modulos separados:
   - `gbcemu-core`
   - `gbcemu-desktop`
   - `gbcemu-android`
2. Manter Maven para core/desktop.
3. Usar um modulo Android com Gradle/Android plugin, chamado por um perfil Maven apenas como conveniencia.
4. Comando atual do scaffold Android:

```powershell
gradle :android-app:assembleDebug
```

5. O SDK Android deve ser encontrado por `ANDROID_HOME` ou por um `local.properties` local:

```properties
sdk.dir=C:/Users/user/AppData/Local/Android/Sdk
```

6. Comando Maven de conveniencia atual:

```powershell
mvn -Pandroid-apk package
```

Esse perfil delega para o Gradle Android e nao deve tentar empacotar Swing/AWT dentro do APK.

Observacao: o build Android deve preferir um JDK/JBR comum. Como o ambiente tambem usa GraalVM para native-image, o projeto fixa `org.gradle.java.home=C:/Users/user/.jdks/ms-25.0.2` e mantem `android.disableJdkImageTransform=true` em `gradle.properties` para evitar que o Gradle Android use o `jlink` do GraalVM.

## Funcionalidades Android Planejadas

- Controles virtuais na tela.
- [x] Primeira ligacao do APK ao core real por `Console`/`Emulator`.
- [x] Renderizacao de frame PPU em `SurfaceView`.
- [x] Audio por `AudioTrack`.
- [x] Mapeamento inicial de D-pad/botoes Android para o input do Game Boy.
- [x] Seletor Android de ROM e copia local inicial.
- [x] Save `.sav` basico junto da copia privada da ROM.
- Perfil de controles por jogo.
- Rumble nativo quando o cartucho suportar.
- Layout horizontal/vertical.
- Save states e rewind com botoes de toque.
- Seletor de ROMs recentes.
- Opcoes de filtro grafico e escala.
- Modo turbo com botao toggle.

## Proxima Fatiamento Tecnico

1. Criar um modulo Java puro para o core, excluindo `Main`, `gui`, Java Sound desktop e dependencias opcionais de `input4j`.
2. Fazer o desktop depender do modulo core e manter `gui.audio`/Swing/input4j no artefato desktop.
3. Fazer o `android-app` depender desse core em vez de consumir o source set principal filtrado.
