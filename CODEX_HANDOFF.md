# CODEX_HANDOFF — Documento Mestre de Onboarding para Continuidade no Codex

## 1. Bem-vindo ao Projeto Priv!
Este documento foi elaborado para orientar o agente **Codex** (ou qualquer desenvolvedor responsável por dar continuidade ao projeto) na transição e evolução do aplicativo **Priv**, originalmente construído no ambiente Google AI Studio.

---

## 2. Visão do Produto & Proposta de Valor
O **Priv** é uma **plataforma de memórias pessoais em Android Nativo** projetada para guardar, organizar, contextualizar e revisitar áudios, mensagens de voz, imagens e momentos especiais recebidos em aplicativos de mensagens (como WhatsApp e Instagram).

### Conceito Chave:
- **"Memória, não Armazenamento"**: Não é um simples gravador de áudio nem uma pasta de arquivos soltos. Cada mídia é associada a uma Pessoa, a um Momento/Evento, a Notas Pessoais e Tags.
- **"Privacidade por Padrão"**: Não possui feed público, seguidores ou anúncios. Todos os dados são isolados por usuário usando Row Level Security (RLS) no banco em nuvem.
- **"Captura Rápida via ACTION_SEND"**: O usuário compartilha um áudio do WhatsApp direto para o Priv com 1 toque e salva imediatamente.

---

## 3. Estado Atual do Código

| Componente | Estado | Localização Principal no Código |
| :--- | :--- | :--- |
| **Interface Jetpack Compose / Material 3** | ✅ 100% Concluído | `ui/screens/`, `ui/components/`, `ui/theme/` |
| **Captura Compartilhada (WhatsApp)** | ✅ 100% Concluído | `MainActivity.kt` + `ui/components/QuickCaptureSheet.kt` |
| **Banco de Dados Room Local** | ✅ 100% Concluído | `data/database/PrivDatabase.kt` + `data/entity/` + `data/dao/` |
| **Edição e Lixeira com Diálogo de Confirmação** | ✅ 100% Concluído | `ui/components/MemoryCardItem.kt` + `ui/screens/LibraryScreen.kt` |
| **Player de Áudio & Ondas Sonoras** | ✅ 100% Concluído | `ui/audio/AudioPlayerManager.kt` + `ui/components/AudioWaveform.kt` |
| **Pessoas, Momentos e Coleções** | ✅ 100% Concluído | `ui/screens/PersonsAndGroupsScreen.kt`, `MomentsAndCollectionsScreen.kt` |
| **Importador de Contatos do Celular** | ✅ 100% Concluído | `data/contacts/ContactsImporter.kt` |
| **Autenticação & Sync Engine (Supabase)** | ✅ 100% Concluído | `data/sync/SupabaseClientManager.kt` + `data/worker/` |
| **PostgreSQL & Migration RLS Script** | ✅ 100% Concluído | `app/supabase_migrations_v1.sql` |
| **Backup Local ZIP / JSON** | ✅ 100% Concluído | `data/export/MemoryExporter.kt` |
| **Suíte de Testes Robolectric** | ✅ 100% Concluído | `app/src/test/java/com/example/priv/SyncAndIsolationTest.kt` |

---

## 4. Tecnologias & Stack
- **Linguagem**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose com Material Design 3 (Identidade do Priv)
- **Persistência Local**: Room SQLite (KSP) + Local Media Files Storage
- **Nuvem**: Supabase PostgreSQL (com RLS) + Supabase Auth + Supabase Storage (`memories` bucket)
- **Tarefas em Background**: AndroidX WorkManager com Backoff Exponencial
- **Testes**: JUnit 4 + Robolectric 4.11.1

---

## 5. Como Compilar, Executar e Testar

1. **Abrir a pasta raiz no Android Studio** (Iguana ou superior).
2. **Configuração de Variáveis**:
   - Copie `.env.example` para `.env`.
   - Adicione suas credenciais de teste do Supabase (`SUPABASE_URL` e `SUPABASE_ANON_KEY`).
3. **Sincronizar Gradle & Compilar**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Rodar Testes Unitários Robolectric**:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## 6. Principais Decisões Arquiteturais
1. **Chaves Primárias em UUID**: Todas as entidades Room utilizam UUIDs gerados no cliente para evitar colisões no Sync.
2. **Offline-First com SyncQueue**: Toda alteração grava primeiro no Room SQLite local e enfileira uma tarefa na `sync_queue`. O WorkManager processa os envios para o Supabase de forma transparente.
3. **Lixeira com Duplo Nível de Exclusão**: Memórias deletadas são sinalizadas com `is_in_trash = true`. Podem ser restauradas ou excluídas permanentemente através de um modal de confirmação.

---

## 7. Próximos Objetivos Recomendados para o Codex
1. **Alterar o Application ID**: Trocar `com.aistudio.privmemoria.kxmpzq` em `app/build.gradle.kts` para o namespace comercial definitivo (ex: `br.com.priv.app`).
2. **Conectar ao Supabase de Produção**: Aplicar o script `app/supabase_migrations_v1.sql` no painel Supabase do cliente.
3. **Preparação para Beta Fechado na Google Play**: Configurar assinatura Release em Gradle e gerar AAB/APK para testes em aparelhos físicos.
