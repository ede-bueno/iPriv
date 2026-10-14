# DATABASE — Esquema do Banco de Dados Room SQLite (GuardDay)

## 1. Visão Geral
O GuardDay utiliza o **Room Database 2.6.1** com o compilador **KSP** (Kotlin Symbol Processing). Todas as chaves primárias são geradas como **UUID (Strings)** no cliente, garantindo idempotência e compatibilidade nativa com sincronização offline-first e persistência na nuvem via Supabase PostgreSQL.

- **Nome do Banco:** `priv_memories_db`
- **Versão Atual:** `6` (com suporte a `AudioMemoryEntity`, `TimeCapsuleEntity` e `ScrapbookEntity`)
- **Migração:** `fallbackToDestructiveMigration()` para desenvolvimento ágil com seeds automáticos no `RoomDatabase.Callback`.

---

## 2. Entidades & Tabelas

### `audio_memories` (`AudioMemoryEntity`) — *NOVO*
Armazena os metadados técnicos de memórias de áudio e as transcrições de texto associadas de forma persistente e estruturada no Room.
- `id` (STRING, Primary Key, UUID)
- `title` (STRING, título descritivo do áudio)
- `fileName` (STRING, nome do arquivo físico ex: `.m4a`)
- `filePath` (STRING, caminho absoluto local em `files/local_audio_vault/`)
- `fileUri` (STRING, URI do provedor de arquivos ou mídia)
- `mimeType` (STRING, ex: `audio/m4a`, `audio/aac`)
- `fileSize` (LONG, tamanho do arquivo em bytes)
- `durationMs` (LONG, duração exata em milissegundos)
- `sampleRate` (INT, taxa de amostragem em Hz, padrão 44.100)
- `channels` (INT, 1 para mono, 2 para estéreo)
- `bitrate` (INT, taxa de bits ex: 128.000 bps)
- `waveformData` (STRING, amplitudes normalizadas para renderização de forma de onda)
- `audioChecksum` (STRING, hash SHA-256 de verificação de integridade)
- `transcriptionText` (STRING, texto transcrito completo associado ao áudio)
- `transcriptionStatus` (STRING: `PENDING`, `COMPLETED`, `EDITED`, `FAILED`)
- `transcriptionLanguage` (STRING: `pt-BR`)
- `transcriptionConfidence` (FLOAT, 0.0f a 1.0f)
- `transcriptionEngine` (STRING: `GEMINI_AI`, `LOCAL_AI`, `MANUAL_INPUT`)
- `summary` (STRING, resumo conciso gerado ou manual)
- `tags` (STRING, tags separadas por vírgula)
- `speakerName` (STRING, identificador do locutor ex: "Você", "Vovó Helena")
- `linkedMemoryId` (STRING?, vínculo opcional com a tabela `memories`)
- `isFavorite` (BOOLEAN, indicador de favorito)
- `source` (STRING: `MICROPHONE`, `WHATSAPP`, `FILE_IMPORT`, `SYSTEM`)
- `createdAt` (LONG, Epoch Millis)
- `updatedAt` (LONG, Epoch Millis)

### `time_capsules` (`TimeCapsuleEntity`)
Armazena cápsulas do tempo programadas para abertura e desbloqueio em data futura.
- `id` (STRING, Primary Key, UUID)
- `title` (STRING, título da cápsula)
- `description` (STRING, mensagem ou tema)
- `targetUnlockTimestamp` (LONG, momento no futuro em que a cápsula é aberta)
- `isLocked` (BOOLEAN, bloqueado até atingir a data)
- `mediaItemsJson` (STRING, lista de fotos, vídeos e áudios serializados em JSON)
- `themeStyle` (STRING, estilo visual: retrô, neon, minimalista)
- `createdAt` (LONG)
- `updatedAt` (LONG)

### `scrapbooks` (`ScrapbookEntity`)
Colagens digitais e murais de recortes de fotos e memórias.
- `id` (STRING, Primary Key, UUID)
- `title` (STRING)
- `description` (STRING)
- `styleTheme` (STRING)
- `coverEmojiOrIcon` (STRING)
- `stickersJson` (STRING)
- `createdAt` (LONG)
- `updatedAt` (LONG)

### `memories` (`MemoryEntity`)
Armazena memórias multimídia principais.
- `id` (STRING, Primary Key, UUID)
- `spaceId` (STRING, Foreign Key para `spaces`)
- `title` (STRING)
- `note` (STRING)
- `timestamp` (LONG)
- `source` (STRING: `WhatsApp`, `Manual`, etc.)
- `isFavorite` (BOOLEAN)
- `isArchived` (BOOLEAN)
- `inTrash` (BOOLEAN)
- `trashedAt` (LONG?)
- `primaryPersonId` (STRING?)
- `momentId` (STRING?)
- `collectionId` (STRING?)
- `syncStatus` (STRING: `LOCAL_ONLY`, `SYNCED`, `PENDING_UPLOAD`, `PENDING_DELETE`, `ERROR`)
- `createdAt` (LONG)
- `updatedAt` (LONG)

### `memory_attachments` (`MemoryAttachmentEntity`)
Anexos detalhados (áudio, fotos, vídeos) de cada memória.
- `id` (STRING, Primary Key, UUID)
- `memoryId` (STRING, índice para `memories.id`)
- `type` (STRING: `AUDIO`, `IMAGE`, `VIDEO`, `TEXT`, `LINK`)
- `uri` (STRING)
- `localPath` (STRING)
- `fileSize` (LONG)
- `durationMs` (LONG)
- `waveformData` (STRING)
- `transcription` (STRING)
- `uploadStatus` (STRING: `LOCAL_ONLY`, `SYNCED`, `PENDING`)

### `persons` (`PersonEntity`) & `groups` (`GroupEntity`)
Perfis de pessoas e grupos familiares ou de amigos para categorização emocional.

### `sync_operations` (`SyncOperationEntity`)
Fila local transacional para operações offline que sincronizam com o Supabase quando há conexão.

---

## 3. DAOs e Camada de Acesso Reativo

Todos os DAOs utilizam o padrão moderno do Jetpack Compose:
- **Consultas de leitura:** Retornam `Flow<T>` ou `Flow<List<T>>` para refletir automaticamente alterações no Compose sem refresh manual.
- **Consultas de modificação:** Marcadas como funções `suspend` para execução exclusiva fora da UI Thread (`Dispatchers.IO`).
- **Padrão Repositório:** A interface de usuário acessa os dados através de `AudioMemoryRepository`, `MemoryRepository`, `TimeCapsuleRepository`, etc., desacoplando o Room da UI.
