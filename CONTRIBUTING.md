# Como contribuir

Este é um projeto pessoal, mas issues e pull requests são bem-vindos.

## Antes de abrir um PR

- Abra uma [issue](https://github.com/vitorsilverio/gbcemu/issues) descrevendo o
  problema/ideia primeiro — evita trabalho duplicado ou um PR que não se encaixa na
  direção do projeto.
- Compile e teste com Java 21+ e Maven:

  ```powershell
  mvn test
  ```

- Toda mudança de comportamento vem com teste automatizado cobrindo o caso novo.
- O core (CPU/PPU/APU/MBC) deve ficar livre de dependências de UI (Swing/AWT) — a
  conversão para imagem/áudio da plataforma fica no frontend desktop (`gui`), não no core;
  ver a seção "Frontend Android" do [README](README.md) para o motivo.
- Mantenha o estilo do código existente; não introduza dependências novas sem discutir
  antes na issue.

## Dúvidas

Abra uma issue ou veja a seção de contato no [README](README.md).
