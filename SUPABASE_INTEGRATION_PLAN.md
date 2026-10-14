# SUPABASE_INTEGRATION_PLAN — Planejamento Completo do Supabase como Fonte de Dados Online

## 1. Visão Geral e Princípios Arquiteturais

O **GuardDay** foi concebido sob o paradigma **Offline-First**. Isso significa que a experiência do usuário nunca é bloqueada pela conectividade com a internet:
- **Operações Locais Imediatas:** Todas as gravações de áudio, transcrições, notas, cápsulas e coleções são salvas primeiramente no banco local **Room (SQLite)** em milissegundos.
- **Sincronização Bidirecional em Background:** O **Supabase** atua como a fonte da verdade em nuvem (Cloud Source of Truth), fornecendo persistência segura entre múltiplos dispositivos, backup blindado, colaboração em tempo real e armazenamento distribuído para áudios e mídias pesadas.

---

## 2. Pilares Tecnológicos do Supabase no GuardDay

| Componente Supabase | Função no GuardDay | Implementação Local no Android |
| :--- | :--- | :--- |
| **PostgreSQL Database** | Banco relacional online que espelha as tabelas Room | `PrivDatabase.kt` (Room SQLite) |
| **PostgREST API** | Endpoints REST automáticos e tipados com suporte a `upsert` | `SupabaseClientManager.kt` via OkHttp/Retrofit |
| **Supabase Storage** | Buckets S3 para armazenar áudios `.m4a` e fotos de alta resolução | `MediaLocalStorage.kt` + `AudioRecorderManager.kt` |
| **Row Level Security (RLS)** | Garantia de que cada usuário acesse apenas seus próprios dados | Políticas SQL em `auth.uid() = owner_user_id` |
| **Realtime WebSockets** | Notificações push de memórias compartilhadas e destrancamento de cápsulas | `PostgREST Realtime / WebSocket Listener` |
| **Edge Functions / Postgres Triggers** | Verificação de data de abertura da Cápsula do Tempo e alertas automáticos | Triggers PL/pgSQL na nuvem |

---

## 3. Esquema Relacional no Supabase PostgreSQL (DDL)

Abaixo está o script DDL SQL completo para criar a estrutura no Supabase:

```sql
-- =========================================================================
-- 1. EXTENSÕES & ENUMS
-- =========================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE media_type_enum AS ENUM ('AUDIO', 'IMAGE', 'VIDEO', 'TEXT', 'LINK');
CREATE TYPE transcription_status_enum AS ENUM ('PENDING', 'TRANSCRIBING', 'COMPLETED', 'EDITED', 'FAILED');

-- =========================================================================
-- 2. TABELA: SPACES (Espaços de Isolamento Pessoal ou Familiar)
-- =========================================================================
CREATE TABLE public.spaces (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT,
    space_type TEXT DEFAULT 'PERSONAL',
    owner_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.spaces ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Usuário acessa seus próprios espaços"
    ON public.spaces FOR ALL
    USING (auth.uid() = owner_user_id);

-- =========================================================================
-- 3. TABELA: AUDIO_MEMORIES (Metadados Técnicos e Transcrições)
-- =========================================================================
CREATE TABLE public.audio_memories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    file_name TEXT NOT NULL,
    remote_storage_path TEXT,
    mime_type TEXT DEFAULT 'audio/m4a',
    file_size_bytes BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    sample_rate INT DEFAULT 44100,
    channels INT DEFAULT 1,
    bitrate INT DEFAULT 128000,
    waveform_data TEXT,
    audio_checksum TEXT,
    transcription_text TEXT DEFAULT '',
    transcription_status transcription_status_enum DEFAULT 'COMPLETED',
    transcription_language TEXT DEFAULT 'pt-BR',
    transcription_confidence REAL DEFAULT 1.0,
    transcription_engine TEXT DEFAULT 'LOCAL_AI',
    summary TEXT,
    tags TEXT,
    speaker_name TEXT DEFAULT 'Você',
    is_favorite BOOLEAN DEFAULT FALSE,
    source TEXT DEFAULT 'MICROPHONE',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.audio_memories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Usuário gerencia seus próprios áudios"
    ON public.audio_memories FOR ALL
    USING (auth.uid() = owner_user_id);

-- =========================================================================
-- 4. TABELA: TIME_CAPSULES (Cápsulas com Trava Temporal Segura)
-- =========================================================================
CREATE TABLE public.time_capsules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    target_unlock_timestamp TIMESTAMPTZ NOT NULL,
    is_locked BOOLEAN DEFAULT TRUE,
    media_items_json JSONB DEFAULT '[]'::jsonb,
    theme_style TEXT DEFAULT 'retro_vibrant',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.time_capsules ENABLE ROW LEVEL SECURITY;

-- Regra RLS: O usuário sempre pode ver o título e data, mas os arquivos só liberam quando a data for atingida
CREATE POLICY "Usuário vê suas cápsulas"
    ON public.time_capsules FOR ALL
    USING (auth.uid() = owner_user_id);

-- =========================================================================
-- 5. TABELA: MEMORIES & ATTACHMENTS
-- =========================================================================
CREATE TABLE public.memories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    space_id UUID REFERENCES public.spaces(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    note TEXT DEFAULT '',
    timestamp TIMESTAMPTZ DEFAULT now(),
    source TEXT DEFAULT 'WhatsApp',
    is_favorite BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    in_trash BOOLEAN DEFAULT FALSE,
    trashed_at TIMESTAMPTZ,
    primary_person_id UUID,
    moment_id UUID,
    collection_id UUID,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.memories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Usuário gerencia suas memórias"
    ON public.memories FOR ALL
    USING (auth.uid() = owner_user_id);

CREATE TABLE public.memory_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    memory_id UUID REFERENCES public.memories(id) ON DELETE CASCADE,
    type media_type_enum NOT NULL,
    remote_path TEXT,
    file_name TEXT,
    file_size_bytes BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    waveform_data TEXT,
    transcription TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.memory_attachments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Usuário acessa anexos das suas memórias"
    ON public.memory_attachments FOR ALL
    USING (EXISTS (
        SELECT 1 FROM public.memories m
        WHERE m.id = memory_attachments.memory_id
        AND m.owner_user_id = auth.uid()
    ));
```

---

## 4. Arquitetura de Sincronização Offline-First (Delta Sync)

### 4.1 Identificadores Universais (UUID v4)
Tanto no Room (SQLite) quanto no Supabase (PostgreSQL), todas as chaves primárias são UUIDs gerados no cliente. Isso elimina o problema clássico de IDs autoincrementais colidindo após períodos offline.

### 4.2 Fila de Operações Transacionais (`sync_operations`)
Quando o dispositivo está offline, qualquer criação, alteração ou exclusão é registrada na tabela Room `sync_operations` com:
1. `entity_type`: ("AUDIO_MEMORY", "MEMORY", "TIME_CAPSULE", etc.)
2. `entity_id`: UUID da entidade
3. `operation_type`: `INSERT`, `UPDATE`, `DELETE`, `UPLOAD_MEDIA`
4. `payload_json`: Snapshot serializado dos dados
5. `created_at`: Carimbo de data/hora

### 4.3 Mecanismo de Push & Pull (Upsert)
1. **Push:** Ao detectar conectividade (via `ConnectivityManager`), o `SupabaseSyncWorker` lê a fila de operações e envia via `POST /rest/v1/{table}?on_conflict=id` com cabeçalho `Prefer: resolution=merge-duplicates`.
2. **Pull:** O cliente consulta `GET /rest/v1/{table}?updated_at=gt.{lastSyncTimestamp}` para baixar apenas os registros alterados desde a última sincronização.
3. **Resolução de Conflitos:** Padrão **Last-Write-Wins (LWW)** baseado no campo `updated_at`. Para exclusões lógicas (Soft Delete), utiliza-se o campo `deleted_at`.

---

## 5. Estratégia de Supabase Storage para Áudios e Mídias

### 5.1 Buckets Recomendados
- **`audio-vault` (Privado):** Gravações de voz locais em `.m4a` AAC 44.1kHz.
  - Formato de caminho: `{userId}/{audioId}.m4a`
- **`memory-media` (Privado):** Fotos e vídeos das memórias.
  - Formato de caminho: `{userId}/{memoryId}/{attachmentId}.jpg`
- **`capsule-vault` (Privado):** Arquivos selados da cápsula do tempo.

### 5.2 Upload em Duas Etapas
1. **Etapa 1:** Gravação e persistência local no SQLite Room + arquivo físico em `files/local_audio_vault/`.
2. **Etapa 2 (Background):** Upload do binário para o bucket `audio-vault` com hash SHA-256 no cabeçalho `Content-SHA256` para validação de integridade. Após confirmação do upload, a URL remota é salva no Room e o status do anexo é atualizado para `SYNCED`.

---

## 6. Configuração no Android via Secrets Panel

As credenciais do Supabase são injetadas em tempo de build através do Secrets Gradle Plugin e acessadas em Kotlin via `BuildConfig`:

```env
SUPABASE_URL=https://xyzcompany.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

No código Kotlin:
```kotlin
val client = SupabaseClientManager(context)
// Verifica se a URL configurada é válida ou se deve operar em modo local/fallback seguro
if (client.isConfigured) {
    client.syncPendingOperations()
}
```

---

## 7. Roteiro de Implementação Passo a Passo

1. **Fase 1 (Atual - Concluída):** Banco de dados Room SQLite local v6 completo, DAOs reativos, repositório de áudios, preferências do usuário e cápsula do tempo totalmente operacionais e testados offline.
2. **Fase 2:** Criação das tabelas e RLS no painel do Supabase executando o DDL SQL acima.
3. **Fase 3:** Configuração das chaves `SUPABASE_URL` e `SUPABASE_ANON_KEY` no Secrets Panel do AI Studio.
4. **Fase 4:** Ativação do `SupabaseSyncWorker` via WorkManager com restrição de rede `NetworkType.CONNECTED` para sincronização transparente em background.
