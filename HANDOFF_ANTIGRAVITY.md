# 📦 GuardDay — Documento Oficial de Handoff & Arquitetura (Exportação para Antigravity)

---

## 1. 🌟 Visão Geral do Produto

**GuardDay** é uma plataforma afetiva para preservação de memórias pessoais, histórias orais, momentos fotográficos e vínculos emocionais antes que o tempo os apague.

Diferente de redes sociais ou cofres digitais frios, o GuardDay foi desenhado sob uma filosofia de **calor humano, privacidade absoluta e conexão emocional**.

* **Nome do App:** GuardDay
* **Pacote Android (ApplicationId):** `com.aistudio.guardday`
* **Plataforma:** Android Nativo (Kotlin + Jetpack Compose)
* **Design System:** Material Design 3 (M3) com Cores Dinâmicas e Temas Afetivos
* **Persistência Principal:** Room Database (100% Offline-First)

---

## 2. 🎨 Identidade Visual & O Novo Ícone Emocional

### O Conceito Visual do Ícone (`ic_launcher_foreground.xml` & `ic_launcher_background.xml`):
* **Ruptura com o Clichê de Segurança Fria:** Em vez de cadeados mecânicos, cofres ou escudos bélicos, a identidade transmite **cuidado e acolhimento**.
* **O Abraço Guardião:** Duas curvas orgânicas entrelaçadas como mãos em concha ou asas de acolhimento (em gradiente suave de rosa-magenta `#FF7597` para lavanda `#9D4EDD`).
* **O Coração de Memórias:** Um núcleo central brilhante em degradê pôr do sol (ouro âmbar `#FFEAA7` a coral vivo `#FF5252`), representando lembranças vivas.
* **Ondas Afetivas de Histórias e Risadas:** Arcos superiores simbolizando a voz de quem amamos e histórias preservadas.
* **Estrela Guia da Nostalgia:** Brilho pontual no topo direito para marcar momentos eternos.
* **Fundo Crepúsculo Aconchegante:** Gradiente noturno profundo (`#261542` a `#0F081C`) que confere sofisticação e conforto visual.

---

## 3. 🏛️ Arquitetura de Software & Estrutura de Código

O projeto adota os padrões modernos recomendados pelo Google para Android:
* **Padrão de Arquitetura:** MVVM (Model-View-ViewModel) + Clean Architecture
* **Fluxo Unidirecional de Dados (UDF):** Estados expostos via `StateFlow` e eventos tratados via `ViewModel`
* **Programação Assíncrona:** Kotlin Coroutines & Kotlin Flow reativo
* **Injeção de Dependências:** Construtores diretos e centralização de instâncias no Singleton do `PrivDatabase`

```
app/src/main/java/com/example/priv/
├── data/
│   ├── database/
│   │   └── PrivDatabase.kt              # RoomDatabase central (Room v2.6+)
│   ├── dao/
│   │   ├── MemoryDao.kt                 # Consultas e inserções de memórias e anexos
│   │   ├── AudioMemoryDao.kt            # Gravações de áudio, metadados e transcrições
│   │   ├── ScrapbookDao.kt              # Murais de lembranças e colagens
│   │   └── PersonDao.kt                 # Pessoas queridas e círculos de afeto
│   ├── entity/
│   │   ├── MemoryEntity.kt              # Entidade principal de memórias
│   │   ├── AttachmentEntity.kt          # Fotos, mídias e arquivos anexados
│   │   ├── AudioMemoryEntity.kt         # Áudios com waveform, autor e duração
│   │   ├── ScrapbookEntity.kt           # Álbuns e murais interativos
│   │   └── PersonEntity.kt              # Perfis de familiares e melhores amigos
│   ├── model/
│   │   ├── MemoryModels.kt              # Modelos de domínio, filtros e tags
│   │   ├── AudioMemoryModels.kt         # Estruturas de reprodução e gravação
│   │   └── ScrapbookModels.kt           # ScrapbookPageItem, Polaroids e Bilhetes
│   ├── repository/
│   │   ├── MemoryRepository.kt          # Repositório de memórias e busca
│   │   ├── AudioMemoryRepository.kt     # Repositório de arquivos de voz
│   │   └── ScrapbookRepository.kt       # Persistência e manipulação de murais
│   └── seed/
│       └── SeedData.kt                  # Dados iniciais acolhedores e funcionais
│
├── ui/
│   ├── screens/
│   │   ├── TimelineScrapbookScreen.kt   # Linha do tempo, álbuns e canvas interativo
│   │   ├── AudioMemoriesScreen.kt       # Gravador de voz e cofre de áudios
│   │   ├── VaultScreen.kt               # Cofre seguro com PIN/Biometria
│   │   └── ...                          # Telas de detalhes e memórias
│   ├── components/
│   │   ├── EmotionalComponents.kt       # Cards de afeto, botões e headers
│   │   └── AudioWaveformVisualizer.kt   # Visualizador de ondas sonoras
│   ├── viewmodel/
│   │   ├── TimelineScrapbookViewModel.kt
│   │   ├── AudioMemoryViewModel.kt
│   │   └── MemoryViewModel.kt
│   └── theme/
│       ├── Color.kt                     # Paleta de tons acolhedores (M3)
│       ├── Theme.kt                     # Configuração de tema escuro/claro M3
│       └── Type.kt                      # Tipografia moderna e legível
│
└── MainActivity.kt                      # Ponto de entrada com Edge-to-Edge e Insets
```

---

## 4. 🚀 Módulos e Recursos Principais

### A. Álbum de Memórias Afetivo (Scrapbook Interativo)
* **Cartões com Ação Imediata (*Action Slots*):** Templates inteligentes como *"Diário Visual dos Melhores Amigos"* já vêm com cartões interativos (*Foto com Amigo(a)*, *Momento Engraçado*, *Trilha Sonora*). Ao tocar, transformam-se na foto ou bilhete escolhido instantaneamente.
* **Cópia Local Segura de Imagens:** Fotos da galeria são salvas em `context.filesDir/scrapbook_images`, garantindo persistência vitalícia mesmo se o URI do Android expirar ou o aparelho reiniciar.
* **Estética Polaroid & Washi Tape:** Molduras vintage com fita adesiva decorativa, inclinação sutil (-2.5° a +2.5°) e toque para editar legendas ou girar.
* **Bilhetes de Papel Pastel:** 5 tonalidades carinhosas (*Rosa, Creme, Céu, Menta, Lavanda*) com autor e mensagem.
* **Doca Flutuante Ergonômica:** Ações rápidas acessíveis com o polegar (+ Foto, + Bilhete, + Adesivo, + Espaço Temático, Fundo).

### B. Linha do Tempo & Cápsulas do Tempo
* Filtros por pessoa querida, data e sentimento.
* Cápsulas do tempo lacradas com contador regressivo que só abrem na data estipulada no futuro.

### C. Cofre de Vozes & Histórias Orais (Audio Memories)
* Gravador de voz embutido com amostragem de amplitude em tempo real.
* Categorização de falas de avós, pais, parceiros(as) e amigos.
* Metadados de contexto (onde foi gravado, emoção sentida, data exata).

---

## 5. ⚡ Performance e Qualidade de Código

1. **Eficiência de Renderização:**
   * Todas as listas `LazyColumn` e `LazyVerticalGrid` usam identificadores estáveis (`key = { it.id }`) para evitar recomposições desnecessárias.
   * `remember` e `derivedStateOf` utilizados em transformações de coleções e cálculos de zoom/pan.
2. **Gerenciamento de Recursos de Mídia:**
   * `MediaPlayer` e `MediaRecorder` com ciclo de vida rigoroso através de `DisposableEffect` e `try-finally`.
   * Coil `AsyncImage` configurado com crossfade e decodificação assíncrona.
3. **Acessibilidade & Boas Práticas:**
   * Áreas de toque de no mínimo 48dp em botões e seletores.
   * `contentDescription` em todos os ícones e elementos visuais interativos.
   * TestTags em formato `snake_case` (ex: `btn_scrapbook_add_gallery_photo`) para testes automatizados.

---

## 6. 🛠️ Instruções para Execução e Build no Antigravity / Android Studio

* **Versão do Android SDK:**
  * `minSdk`: 26 (Android 8.0 Oreo)
  * `targetSdk`: 35 (Android 15)
  * `compileSdk`: 35
* **Java/JVM Target:** Java 17
* **Kotlin:** 2.0+ com Jetpack Compose Compiler integrado

### Comandos de Verificação:
```bash
# Compilar o APK de Debug:
gradle assembleDebug

# Rodar os testes unitários:
gradle :app:testDebugUnitTest
```

---
*Documento preparado para a transição completa e exportação para o ambiente Antigravity.*
