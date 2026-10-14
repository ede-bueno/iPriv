# KNOWN_LIMITATIONS — Limitações Técnicas e Débitos Conhecidos

## 1. Visão Geral
Este documento descreve as limitações atuais do ecossistema do aplicativo Priv e os débitos técnicos intencionais assumidos para garantir o encerramento limpo do ciclo de desenvolvimento no Google AI Studio.

---

## 2. Limitações Atuais

### A. Transcrição de Áudio Automatizada (IA)
- **Status**: Não implementada no MVP (Conforme escopo intencional de produto).
- **Detalhe**: O campo `transcription` existe no banco de dados e a interface da UI exibe o botão de transcrição expansível, porém a integração com modelo de speech-to-text (Whisper/Gemini) está postergada para a versão V2.

### B. Cliente HTTP Nativo Supabase vs SDK Kotlin
- **Status**: Comunicação via OkHttp/Retrofit direto nos endpoints REST do PostgREST e Storage API.
- **Recomendação**: Para novos desenvolvimentos no Codex, recomenda-se adotar o SDK oficial Supabase Kotlin (`io.github.jan-tennert.supabase:postgrest-kt`) para reduzir código boilerplate de parsing JSON manual.

### C. Captura de Áudio em Background no WhatsApp
- **Status**: Requer ação manual do usuário de clicar em "Compartilhar" no WhatsApp e escolher o ícone do Priv.
- **Detalhe**: O Android por razões de segurança proíbe a leitura automática em segundo plano do banco de dados do WhatsApp.

---

## 3. Débitos Técnicos Monitorados

1. **Namespace / Application ID do Build**:
   - Atualmente configurado como `com.aistudio.privmemoria.kxmpzq` em `app/build.gradle.kts`.
   - Ação para o Codex: Alterar para o Application ID final desejado antes da publicação em produção.

2. **Migração Room Automática**:
   - A versão do banco está fixada em `v1`. Alterações futuras de tabela devem utilizar scripts de `Migration` do Room para evitar perda de dados dos usuários em atualização de app.
