# PROJECT_STRUCTURE — Mapeamento Completo da Estrutura de Diretórios (GuardDay)

## 1. Mapa Global de Pastas

```
/ (Raiz do Projeto)
├── .env.example                       # Modelo de variáveis de ambiente para o Supabase e Gemini
├── .gitignore                         # Arquivos ignorados no Git
├── build.gradle.kts                   # Script de build da raiz (Plugins Kotlin/AGP/KSP)
├── settings.gradle.kts                # Configuração do projeto e repositórios
├── metadata.json                      # Metadados da plataforma AI Studio ("GuardDay")
├── ARCHITECTURE.md                    # Arquitetura Clean + MVVM + Offline-First Room + Supabase
├── DATABASE.md                        # Esquema do banco de dados Room SQLite v6
├── SUPABASE_INTEGRATION_PLAN.md       # Planejamento completo do Supabase como backend online
├── SUPABASE_SETUP.md                  # Instruções de setup e DDL PostgREST
├── CURRENT_STATUS.md                  # Status atualizado dos módulos
├── PROJECT_STRUCTURE.md               # Este documento
├── README.md                          # Visão geral e instruções rápidas
│
├── gradle/
│   └── libs.versions.toml             # Version Catalog centralizado do Gradle (Room, Compose, Coil, etc.)
│
└── app/
    ├── build.gradle.kts               # Script de build do módulo app (Room 2.6.1 + KSP, Compose M3, Secrets)
    ├── proguard-rules.pro             # Regras de otimização ProGuard/R8
    │
    └── src/
        ├── test/                      # Testes Unitários de JVM e Robolectric
        │   └── java/com/example/priv/
        │       └── ExampleRobolectricTest.kt
        │
        └── main/
            ├── AndroidManifest.xml    # Manifesto com RECORD_AUDIO, INTERNET, READ_CONTACTS, FileProvider
            │
            ├── res/                   # Recursos Android (Strings, Cores, Ícones, XML Provider Paths)
            │
            └── java/com/example/
                ├── MainActivity.kt    # Entrypoint Principal, Tema Reativo, Insets e Navegação
                │
                ├── ui/theme/
                │   ├── Color.kt       # Paleta de Cores Material 3 (Tons dinâmicos, light e dark)
                │   ├── Theme.kt       # PrivTheme / GuardDayTheme com suporte a AMOLED Dark
                │   └── Type.kt        # Tipografia escalável
                │
                └── priv/
                    ├── auth/
                    │   └── GoogleAuthManager.kt # Credential Manager e autenticação Google
                    │
                    ├── data/
                    │   ├── ai/
                    │   │   └── PrivGeminiIntelligenceManager.kt # IA Generativa e transcrições em português
                    │   │
                    │   ├── contacts/
                    │   │   └── ContactsImporter.kt # Leitor de agenda local de contatos
                    │   │
                    │   ├── dao/               # DAOs Room com suporte a Kotlin Coroutines Flow
                    │   │   ├── AudioMemoryDao.kt        # DAO de áudios, metadados e transcrições
                    │   │   ├── TimeCapsuleDao.kt        # DAO de cápsulas do tempo programadas
                    │   │   ├── MemoryDao.kt             # DAO de memórias multimídia principais
                    │   │   ├── ScrapbookDao.kt          # DAO de colagens e murais visuais
                    │   │   ├── PersonDao.kt             # DAO de contatos/pessoas
                    │   │   ├── GroupDao.kt              # DAO de grupos
                    │   │   ├── MomentDao.kt             # DAO de momentos especiais
                    │   │   ├── CollectionDao.kt         # DAO de coleções
                    │   │   ├── TagDao.kt                # DAO de tags
                    │   │   ├── SpaceDao.kt              # DAO de espaços isolados
                    │   │   └── SyncQueueDao.kt          # DAO da fila de sincronização offline
                    │   │
                    │   ├── database/
                    │   │   └── PrivDatabase.kt          # Singleton Room Database (priv_memories_db v6)
                    │   │
                    │   ├── entity/            # Entidades Room
                    │   │   ├── AudioMemoryEntity.kt     # Metadados de áudio e transcrição
                    │   │   ├── TimeCapsuleEntity.kt     # Cápsula do tempo e trava agendada
                    │   │   ├── ScrapbookEntity.kt       # Murais e recortes de fotos
                    │   │   ├── MemoryEntity.kt          # Memória principal
                    │   │   ├── MemoryAttachmentEntity.kt# Anexos multimídia
                    │   │   ├── PersonEntity.kt          # Pessoa associada
                    │   │   ├── GroupEntity.kt           # Grupo associado
                    │   │   ├── MomentEntity.kt          # Momento associado
                    │   │   ├── CollectionEntity.kt      # Coleção associada
                    │   │   ├── TagEntity.kt             # Tags
                    │   │   ├── SpaceEntity.kt           # Espaço (pessoal/compartilhado)
                    │   │   ├── SyncOperationEntity.kt   # Item da fila de sync
                    │   │   └── Junctions.kt             # Tabelas Cross-Ref (Many-to-Many)
                    │   │
                    │   ├── export/
                    │   │   └── BackupExportManager.kt   # Exportação e importação de backups JSON
                    │   │
                    │   ├── preferences/
                    │   │   └── UserPreferencesManager.kt# Gerenciador de preferências (StateFlow + SharedPreferences)
                    │   │
                    │   ├── repository/        # Camada de Repositório (Clean Architecture)
                    │   │   ├── AudioMemoryRepository.kt # Repositório de áudios e transcrições
                    │   │   ├── TimeCapsuleRepository.kt # Repositório de cápsulas agendadas
                    │   │   ├── MemoryRepository.kt      # Repositório de memórias principais
                    │   │   ├── ScrapbookRepository.kt   # Repositório de scrapbooks
                    │   │   ├── PersonRepository.kt      # Repositório de pessoas
                    │   │   ├── GroupRepository.kt       # Repositório de grupos
                    │   │   ├── MomentRepository.kt      # Repositório de momentos
                    │   │   └── CollectionRepository.kt  # Repositório de coleções
                    │   │
                    │   ├── security/
                    │   │   └── SecurityLockManager.kt   # Controle de trava por PIN e biometria
                    │   │
                    │   ├── seed/
                    │   │   └── SeedData.kt              # Povoamento inicial rico com áudios e memórias
                    │   │
                    │   ├── storage/
                    │   │   └── MediaLocalStorage.kt     # Manipulação de arquivos locais em cache/files
                    │   │
                    │   └── sync/
                    │       ├── SupabaseClientManager.kt # Cliente PostgREST, Storage e Auth do Supabase
                    │       └── ConflictResolver.kt      # Resolução de conflitos de sincronização
                    │
                    ├── util/
                    │   └── audio/
                    │       └── AudioRecorderManager.kt  # Utilitário de gravação AAC/M4A, amplitude e arquivos
                    │
                    └── ui/
                        ├── audio/
                        │   ├── AudioPlayerManager.kt        # Player de áudio com progresso reativo
                        │   ├── AudioRecordingBottomSheet.kt # Gravação interativa com microfone em tempo real
                        │   └── AudioMemoriesModalSheet.kt   # Cofre de áudios, busca por transcrição e player
                        │
                        ├── components/
                        │   ├── QuickCaptureSheet.kt         # Captura rápida de voz, fotos e textos
                        │   ├── MemoryCardItem.kt            # Card de memória com transcrições e áudio
                        │   ├── MemoryDetailModalSheet.kt    # Visualizador e editor detalhado
                        │   ├── MiniAudioPlayerBar.kt        # Barra fixa de reprodução de áudio
                        │   └── AppLockOverlay.kt            # Tela de bloqueio por PIN
                        │
                        ├── preferences/
                        │   └── UserPreferencesMenuSheet.kt  # Menu completo de preferências do usuário
                        │
                        ├── screens/
                        │   ├── HomeScreen.kt                # Feed principal, recordações e atalhos
                        │   ├── LibraryScreen.kt             # Biblioteca de memórias com filtros
                        │   ├── ProfileScreen.kt             # Perfil, preferências, backup e cofre de áudio
                        │   ├── PersonsAndGroupsScreen.kt    # Gestão de pessoas e círculos
                        │   ├── MomentsAndCollectionsScreen.kt # Linha do tempo de momentos
                        │   ├── TimelineScrapbookScreen.kt   # Cápsula do tempo e murais de recortes
                        │   └── CreateTimeCapsuleFullScreenDialog.kt # Criação e agendamento de cápsula
                        │
                        └── viewmodel/
                            ├── HomeViewModel.kt
                            ├── LibraryViewModel.kt
                            ├── QuickCaptureViewModel.kt
                            ├── ProfileViewModel.kt
                            ├── PersonViewModel.kt
                            ├── GroupsViewModel.kt
                            ├── MomentsViewModel.kt
                            └── TimelineScrapbookViewModel.kt
```
