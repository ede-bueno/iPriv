# Reformulação de UX & UI: GuardDay (Priv)

Este plano estabelece a modernização visual e ergonômica completa do GuardDay, integrando uma navegação clara, posicionamento intuitivo do menu do usuário e um design minimalista moderno em tons obsidian e lavanda suave, mantendo intactas todas as funcionalidades existentes de banco de dados Room (busca por transcrição e tags), gravação e reprodução de áudio.

---

## Revisão do Usuário & Decisões Críticas

> [!IMPORTANT]
> As seguintes escolhas de experiência e visual foram confirmadas na etapa de alinhamento e servem como pilares desta reformulação:

- **Menu do Usuário Acessível**: Avatar do usuário fixo no topo direito em todas as telas da aplicação (`TopAppBar` unificada global), abrindo uma folha/gaveta com perfil, preferências e sincronização Supabase com 1 toque.
- **Identidade Visual**: Minimalista moderno com tons Obsidian (fundos escuros profundos, superfícies elegantes com baixo contraste reflexivo) e detalhes em Lavanda Suave (accent, seletores, destaque de áudio e tags).
- **Estrutura de Navegação**: Barra inferior limpa com 4 abas estratégicas (Início, Pessoas, Memórias e Linha do Tempo), eliminando poluição visual e garantindo acesso rápido a todas as funções.
- **Preservação de Recursos (Zero Perda)**: Todas as implementações existentes são preservadas integralmente — incluindo a busca Room FTS/LIKE por título e transcrições, sistema de tags nas memórias, waveform de áudio, gravação de voz e gestão de círculos de pessoas.

---

## 1. Visão Geral & Conceito Central

- **O que faz**: O GuardDay é um diário e cofre de memórias pessoais e familiares de alta privacidade. Permite gravar áudios com transcrição instantânea, catalogar momentos com fotos e pessoas queridas, organizar através de tags inteligentes e pesquisar rapidamente em memórias passadas.
- **Público-Alvo**: Pessoas que valorizam a preservação de memórias afetivas, áudios com entes queridos e diários sonoros de maneira íntima, discreta e segura.
- **Valor Principal**: Uma interface calma, silenciosa e refinada que não distrai, com ferramentas potentes de busca e reprodução de áudio e acesso transparente ao perfil e configurações a qualquer momento.

---

## 2. Experiência do Usuário & Design Visual

### Fluxos Principais do Usuário
1. **Acesso Global ao Perfil/Menu**:
   - Em qualquer tela (Início, Pessoas, Memórias, Linha do Tempo), o avatar no canto superior direito mostra o status de sincronização e foto/iniciais do usuário.
   - Ao tocar no avatar, abre-se instantaneamente o `UserPreferencesMenuSheet` / diálogo de perfil com configurações de conta, tema, privacidade e biometria.
2. **Busca e Exploração de Memórias**:
   - Barra de busca fluida na aba "Memórias" (e acessível na Home) com filtro instantâneo por texto (título ou conteúdo da transcrição) e seleção rápida de chips de tags (`#família`, `#viagem`, `#trabalho`, etc.).
   - Reprodução inline de memórias com visualizador de waveform animado em lavanda suave.
3. **Criação e Gravação**:
   - Botão de ação flutuante (FAB) proeminente e harmônico na base para iniciar gravação ou nova memória com microfone e tags imediatas.

### Identidade Visual & Tema (Obsidian & Lavanda Suave)
- **Superfície & Fundos (Obsidian)**:
  - Fundo principal: `#12111A` (Obsidian profundo, quente e suave aos olhos).
  - Superfícies de cartões e modais: `#1C1A27` e `#242233`.
  - Bordas sutis: `#2E2A40` (1dp com toque acetinado).
- **Cores de Destaque (Lavanda Suave)**:
  - Primária: `#C5B4E3` (Lavanda claro e luminoso).
  - Secundária / Accent: `#E2D9F3` e `#9A88C4`.
  - Gradiente de áudio: Transição sutil entre `#A792CF` e `#C5B4E3`.
- **Tipografia**:
  - Títulos elegantes com peso `SemiBold` e kerning ligeiramente expandido para sensação editorial e moderna.
  - Textos de transcrição com espaçamento de linha confortável (`lineHeight = 22.sp`) para leitura agradável.
- **Formas & Espaçamento**:
  - Cantos arredondados consistentes: 12.dp para chips e botões, 18.dp para cartões de memória, 24.dp para diálogos e bottom sheets.
  - Sistema de espaçamento baseado na grade de 8.dp (paddings de 16.dp nas bordas, 12.dp entre elementos internos).

---

## 3. Decisões Principais & Soluções de Arquitetura

### Decisão 1: Avatar Fixo no Topo Direito (Substituindo o antigo ícone isolado de configurações)
- **Abordagem Escolhida**: Implementar uma barra superior fixa (`GuardDayTopBar`) gerenciada no `Scaffold` raiz em `MainActivity`, recebendo o título contextual da aba ativa e exibindo permanentemente o `UserAvatarButton`.
- **Por que**: Elimina a inconsistência onde o menu só existia na tela inicial ou como chip rolável escondido. O usuário agora encontra seu perfil e preferências sempre no mesmo lugar em qualquer aba.
- **Alternativas Consideradas**: Adicionar uma 5ª aba na barra inferior (rejeitada para não poluir o bottom navigation) ou gaveta lateral (rejeitada pois exige gesto de arrasto pouco intuitivo para preferências rápidas).

### Decisão 2: Estrutura da Barra Inferior com 4 Abas Limpas
- **Abordagem Escolhida**: Barra inferior com 4 itens:
  1. **Início** (`Home`): Destaques recentes, lembretes de momentos e atalho para gravação rápida.
  2. **Pessoas** (`People`): Círculos familiares, amigos e memórias agrupadas por pessoa.
  3. **Memórias** (`Memories`): Biblioteca completa com o mecanismo de busca Room (título, transcrições e tags).
  4. **Linha do Tempo** (`Timeline`): Visualização cronológica contínua estilo scrapbook.
- **Por que**: Cria uma divisão mental clara entre descoberta/feed, pessoas queridas, busca de acervo e cronologia.

### Decisão 3: Mecanismo de Busca Room e Marcação de Tags
- **Abordagem Escolhida**: Preservar a DAO Room com query estendida:
  `WHERE title LIKE :query OR transcription LIKE :query OR tags LIKE :query`
  e suporte a filtro cruzado por chips de tags.
- **Por que**: Desempenho nativo e offline-first de busca rápida no dispositivo sem dependência de rede.

---

## 4. Arquitetura Técnica & Fluxo de Componentes

### Diagrama de Componentes do Sistema

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Scaffold)                         │
├────────────────────────────────────────────────────────────────────────┤
│  TopAppBar Global: [ Logotipo / Título da Aba ]    [ Avatar do Usuário ]│
│                                                            │           │
│                                                            ▼           │
│                                              ┌───────────────────────┐ │
│                                              │ UserPreferencesSheet  │ │
│                                              │ • Perfil & Nome       │ │
│                                              │ • Sincronização Cloud │ │
│                                              │ • Segurança & Tema    │ │
│                                              └───────────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│                          Conteúdo Ativo da Tela                        │
│                                                                        │
│  ┌───────────────┐ ┌────────────────┐ ┌───────────────┐ ┌────────────┐ │
│  │   HomeScreen  │ │  PeopleScreen  │ │ MemoriesScreen│ │  Timeline   │ │
│  │ • Destaques   │ │ • Círculos     │ │ • Busca Room  │ │ • Scrapbook│ │
│  │ • Gravador    │ │ • Familiares   │ │ • Filtro Tags │ │ • Datas    │ │
│  │ • Ações Rápidas│ │ • Fotos       │ │ • Waveform    │ │ • Momentos │ │
│  └───────────────┘ └────────────────┘ └───────────────┘ └────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│     BottomNavigationBar: [ Início ]  [ Pessoas ]  [ Memórias ]  [ Linha ]│
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │                Camada de Dados & Repositório            │
       │  • MemoryDao (Room SQLite): Busca por título/texto/tags  │
       │  • AudioRecorder & Player (MediaPlayer / MediaRecorder) │
       │  • SupabaseSyncManager (Backup & Estado de Nuvem)       │
       └─────────────────────────────────────────────────────────┘
```

### Mapeamento de Estados e Interações

| Componente | Ação do Usuário | Mutação de Estado |
| :--- | :--- | :--- |
| `UserAvatarButton` | Toque no avatar | Alterna `showUserMenuSheet = true` |
| `UserPreferencesMenuSheet` | Edição de perfil ou toggle de tema | Atualiza `UserPreferences` e reflete imediatamente no tema |
| `SearchBar` (Memórias) | Digitação na busca | Atualiza `searchQuery`, reexecuta busca no `MemoryDao` |
| `TagFilterChips` | Toque em chip de tag (`#viagem`) | Alterna seleção de tag no `MemoryViewModel` e filtra lista |
| `MemoryAudioPlayerCard` | Toque em Play/Pause | Inicia áudio via `AudioPlayerHelper`, ativa animação de waveform |
| `BottomNavigationBar` | Troca de aba | Atualiza `selectedTab` com transição suave e preservação de backstack |
