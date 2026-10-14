# ARCHITECTURE — Arquitetura do Sistema Priv

## 1. Visão Geral da Arquitetura
O **Priv** utiliza uma arquitetura Android nativa moderna fundamentada em **Clean Architecture**, **MVVM (Model-View-ViewModel)** e o padrão **Offline-First**.

```
 +-------------------------------------------------------+
 |                 Jetpack Compose UI                    |
 | (HomeScreen, PersonsScreen, LibraryScreen, Player)    |
 +-------------------------------------------------------+
                            │
                            ▼
 +-------------------------------------------------------+
 |                    ViewModels                         |
 | (HomeViewModel, PersonViewModel, LibraryViewModel...) |
 +-------------------------------------------------------+
                            │
                            ▼
 +-------------------------------------------------------+
 |                  Repository Layer                     |
 | (MemoryRepository, PersonRepository, SyncRepository)  |
 +-------------------------------------------------------+
              │                               │
              ▼                               ▼
 +--------------------------+    +--------------------------+
 |       Local Source       |    |      Remote Source       |
 | (Room SQLite + Media)    |    | (Supabase REST + Storage)|
 +--------------------------+    +--------------------------+
```

---

## 2. Princípios de Arquitetura

### A. Offline-First
O banco de dados Room local (`PrivDatabase`) é a **Única Fonte da Verdade (Single Source of Truth)** para a interface do usuário. 
- Qualquer gravação de memória (via `ACTION_SEND` ou entrada manual) é gravada **imediatamente** no Room local e na pasta de mídia local.
- A UI reage instantaneamente via `Flow` / `StateFlow`.
- As operações pendentes são enfileiradas na tabela `sync_queue`.

### B. Sincronização Assíncrona via WorkManager
A comunicação com a nuvem (Supabase) acontece de forma não-bloqueante através de Workers do AndroidX WorkManager:
1. `MetadataSyncWorker`: Sincroniza inserções, edições e exclusões no PostgREST do Supabase.
2. `MediaUploadWorker`: Envia os arquivos de áudio/mídia para o Supabase Storage bucket `memories`.
3. `MediaDownloadWorker`: Baixa mídias faltantes do Supabase Storage após login/restauração em novo dispositivo.
4. `DeletionSyncWorker`: Processa limpeza remota de memórias excluídas permanentemente.

### C. Isolamento de Espaço e Segurança (Multi-Tenant RLS)
Cada usuário pertence a um `Space` (Espaço de Memórias).
- As entidades possuem os campos `space_id` e `owner_user_id`.
- No Supabase, o PostgreSQL impõe **Row Level Security (RLS)** em todas as tabelas. As requisições HTTP enviam o cabeçalho `Authorization: Bearer <user_jwt_token>`.
- Isso garante isolamento estrito: um usuário nunca consegue acessar ou modificar memórias de outro espaço.

---

## 3. Componentes Principais

### Data Layer
- **`PrivDatabase`**: Banco de dados Room v1 com DAOs fortemente tipados (`MemoryDao`, `PersonDao`, `SpaceDao`, `SyncQueueDao`, `MomentDao`, `CollectionDao`, `TagDao`).
- **`MediaLocalStorage`**: Gerenciador de arquivos locais responsável por salvar áudios e imagens em `context.filesDir/media/`, calculando hash SHA-256 para evitar duplicatas.
- **`SupabaseClientManager`**: Cliente HTTP OkHttp / Retrofit para autenticação Supabase Auth, REST API (PostgREST) e Storage API.

### Domain / Repository Layer
- **`MemoryRepository`**: Gerencia criação, consulta, atualização, movimentação para a lixeira, restauração e exclusão permanente de memórias.
- **`PersonRepository`**: Gerencia perfis de pessoas, apelidos, avatares e associação a memórias e eventos.
- **`SyncRepository`**: Controla o enfileiramento e processamento de itens no `sync_queue`.

### UI Layer
- **`AppNavigation`**: Grafo de navegação Compose com `NavHost`, gerenciando transições entre Início, Pessoas, Biblioteca, Momentos e Perfil.
- **`AudioPlayerManager`**: Encapsula reprodução de áudio, cálculo de amplitudes para o visualizador de ondas e controle de estado de reprodução.
- **`QuickCaptureSheet`**: Modal de captura rápida ativado pelo compartilhamento nativo `ACTION_SEND`.
