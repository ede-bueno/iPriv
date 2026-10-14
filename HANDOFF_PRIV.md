# HANDOFF PRIV — Documento Principal de Transferência Técnica

## 1. Visão Geral do Produto
O **Priv** é um aplicativo mobile Android nativo para guardar, organizar, contextualizar e revisitar memórias pessoais (áudios, mensagens de voz, imagens, momentos e notas) recebidas via WhatsApp, Instagram ou outros canais de comunicação rápida.

- **Nome do App**: Priv
- **Application ID atual**: `com.aistudio.privmemoria.kxmpzq` *(Nota: ID de build AI Studio; alterar para o pacote final, ex: `br.com.priv.app`, antes da publicação na Google Play)*
- **Arquitetura Base**: Android Nativo (Kotlin 1.9.24, Jetpack Compose, Clean Architecture / MVVM, Offline-First)
- **Persistência Local**: Room SQLite + File Storage + DataStore
- **Nuvem & Backend**: Supabase (Auth, PostgreSQL com RLS, Storage Privado "memories")
- **Processamento em Segundo Plano**: WorkManager com Exponential Backoff

---

## 2. Estrutura do Repositório

```
.
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── supabase_migrations_v1.sql
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/priv/
│       │   │   ├── auth/              # GoogleAuthManager e Gerenciador de Credenciais
│       │   │   ├── data/
│       │   │   │   ├── ai/            # AIContextManager e serviço de resumos
│       │   │   │   ├── contacts/      # Importador de Contatos Nativo do Android
│       │   │   │   ├── dao/           # Room DAOs (Memory, Person, Space, SyncQueue...)
│       │   │   │   ├── database/      # PrivDatabase (Room)
│       │   │   │   ├── entity/        # Entidades Room com UUIDs
│       │   │   │   ├── export/        # Exportador e Importador de Backup ZIP/JSON
│       │   │   │   ├── model/         # Value Objects, Enums, State Classes
│       │   │   │   ├── repository/    # Interfaces e Implementações de Repositórios
│       │   │   │   ├── security/      # Criptografia e Gerenciamento de PIN/Biometria
│       │   │   │   ├── seed/          # Seed de inicialização de demonstração
│       │   │   │   ├── source/        # Fontes de Dados Locais e Remotas (PostgREST / Storage)
│       │   │   │   ├── storage/       # Manipulação do Armazenamento de Mídia Local
│       │   │   │   ├── sync/          # SupabaseClientManager e Lógica de RLS/Sync
│       │   │   │   └── worker/        # WorkManager (MetadataSync, MediaUpload, MediaDownload, DeletionSync)
│       │   │   ├── ui/
│       │   │   │   ├── audio/         # AudioPlayerManager (Media3 / MediaPlayer)
│       │   │   │   ├── components/    # Componentes Compose (AudioWaveform, MemoryCard, QuickCapture, EditMemoryDialog)
│       │   │   │   ├── navigation/    # Grafo de Navegação Compose (AppNavigation)
│       │   │   │   ├── screens/       # Telas (HomeScreen, PersonsScreen, LibraryScreen, MomentsAndCollectionsScreen, ProfileScreen)
│       │   │   │   ├── theme/         # Design System (Color, Type, Theme Material 3)
│       │   │   │   └── viewmodel/     # ViewModels (HomeViewModel, PersonViewModel, LibraryViewModel, QuickCaptureViewModel, ProfileViewModel)
│       │   │   └── MainActivity.kt    # Entrypoint com Suporte a ACTION_SEND
│       │   └── res/                   # Drawables, Strings, Values
│       └── test/                      # Testes Unitários Robolectric & Suíte RLS (SyncAndIsolationTest)
├── docs/                              # Documentação e Evidências
├── gradle/
│   └── libs.versions.toml             # Version Catalog Centralizado
├── .env.example                       # Modelo de Variáveis para Supabase
├── local.properties.example           # Modelo para SDK Path do Android Studio
├── .gitignore                         # Regras Estritas de Exclusão Git
├── CODEX_HANDOFF.md                   # Guia Mestre de Onboarding para Codex
├── HANDOFF_PRIV.md                    # Este Documento
├── CURRENT_STATUS.md                  # Estado Atual do Sistema
├── ARCHITECTURE.md                    # Arquitetura e Fluxos
├── DATABASE.md                        # Esquema de Banco de Dados Room
├── SUPABASE_SETUP.md                  # Guia do Supabase Backend
└── BUILD_AND_TEST.md                  # Instruções de Compilação e Testes
```

---

## 3. Tecnologias & Bibliotecas Utilizadas

- **Linguagem**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose com Material Design 3
- **Navegação**: Navigation Compose 2.7.7
- **Banco de Dados Local**: Room Database 2.6.1 com KSP
- **Serialização**: kotlinx.serialization 1.6.3
- **Processamento em 2º Plano**: AndroidX WorkManager 2.9.0
- **Rede & HTTP**: OkHttp 4.12.0 / Retrofit 2.9.0
- **Injeção de Variáveis**: Secrets Gradle Plugin 2.0.1 (via `.env`)
- **Testes**: JUnit 4, Robolectric 4.11.1, AndroidX Test Core

---

## 4. Fluxo Offline-First & Sincronização em Nuvem

1. **Captura Rápida (Offline-First)**:
   - Compartilhamento via `ACTION_SEND` (WhatsApp ou gerenciador de arquivos) abre o `QuickCaptureSheet`.
   - O arquivo de áudio ou mídia recebido é copiado imediatamente para o armazenamento privado do app (`files/media/audio/`).
   - É calculado o checksum SHA-256 da mídia e gravada a entidade no Room com status `LOCAL_ONLY`.
   - Um registro de operação é enfileirado na tabela `sync_queue`.

2. **Sincronização com Supabase (WorkManager)**:
   - `MetadataSyncWorker`: Conecta ao PostgREST `/rest/v1/memories` e tabelas auxiliares usando o token do usuário.
   - `MediaUploadWorker`: Realiza upload do arquivo para o bucket privado `memories` no caminho `spaces/{spaceId}/memories/{memoryId}/{attachmentId}.ogg`.
   - `MediaDownloadWorker`: Realiza download sob demanda da mídia quando restaurado em novo dispositivo.
   - `DeletionSyncWorker`: Processa exclusões lógicas/físicas sincronizadas (Lixeira e Exclusão Permanente).

3. **Isolamento de Segurança (RLS)**:
   - Toda query envia o header `Authorization: Bearer <user_access_token>`.
   - As policies RLS no PostgreSQL do Supabase e as Storage Security Policies garantem que usuários de outros espaços (ex: Conta B) recebam HTTP 403 Forbidden ou lista vazia ao tentar acessar dados da Conta A.

---

## 5. Próximos Passos Recomendados no Codex

1. **Migração do Application ID**: Renomear `com.aistudio.privmemoria.kxmpzq` no `build.gradle.kts` para o namespace definitivo do produto (ex: `br.com.priv.app`).
2. **Execução das Migrations SQL**: Rodar o arquivo `app/supabase_migrations_v1.sql` no SQL Editor do seu projeto Supabase real.
3. **Substituição do OkHttp nativo pela lib oficial do Supabase** (`io.github.jan-tennert.supabase:postgrest-kt` e `storage-kt`) se desejado.
4. **Instalação e Validação**: Abrir a pasta no Android Studio, sincronizar o Gradle e rodar no Emulador ou Aparelho Físico.
