# BETA_ISSUES — Relatório de Testes Beta e Mapeamento de Edge Cases

## 1. Resumo do Teste Beta
Durante os testes de integração do MVP do Priv no ambiente emulado e aparelhos de teste, foram avaliados cenários de captura, reprodução, navegação e sincronização em background.

---

## 2. Status dos Testes e Soluções Implementadas

| ID | Área / Módulo | Cenário do Teste | Resultado / Solução |
| :--- | :--- | :--- | :--- |
| **ISSUE-01** | UI / Exclusão | Usuário tocava acidentalmente no ícone da lixeira | **Resolvido**: Adicionado diálogo modal de confirmação tanto na movimentação para a lixeira quanto na exclusão definitiva. |
| **ISSUE-02** | Captura WhatsApp | Áudio compartilhado de pasta temporária do Android sumia após reinicialização | **Resolvido**: O Priv agora copia imediatamente o byte stream do arquivo para `files/media/audio/` sob controle direto do app. |
| **ISSUE-03** | Reprodutor Áudio | Falha ao reproduzir áudio com formato OGG/OPUS enviado pelo WhatsApp em aparelhos antigos | **Resolvido**: `AudioPlayerManager` configurado com decodificação resiliente via Android `MediaPlayer` / `Media3`. |
| **ISSUE-04** | Sync / Permissões | WorkManager falhava ao tentar sincronizar em modo de economia de bateria estrita | **Resolvido**: Adicionado `Constraints.Builder()` exigindo conectividade `CONNECTED` e tratamento de reagendamento exponencial. |
| **ISSUE-05** | Perfil / Backup | Inclusão de fotos grandes no backup ZIP extrapolava memória da JVM | **Resolvido**: Leitura e compactação de streams de arquivo via buffer Okio com limite de alocação de heap. |

---

## 3. Edge Cases Monitorados para o Lançamento Físico
1. **Compartilhamento de múltiplos áudios simultâneos (`ACTION_SEND_MULTIPLE`)**: O app atualmente suporta `ACTION_SEND` de um item individual por vez. Suporte a lote pode ser adicionado no Codex se desejado.
2. **Armazenamento Interno Cheio**: Caso o espaço do dispositivo seja menor que 50MB, o Priv exibe alerta de espaço insuficiente antes de copiar a mídia.
