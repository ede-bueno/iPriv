# Priv — Memória Viva para Suas Vozes, Momentos e Histórias

> **Guarde as vozes, momentos e histórias que não podem ficar perdidos em uma conversa.**

O **Priv** é um aplicativo Android nativo desenvolvido em **Kotlin** e **Jetpack Compose**, projetado para capturar, organizar, contextualizar e revisitar áudios, mensagens de voz, imagens, momentos e memórias significativas recebidas em aplicativos de comunicação como WhatsApp e Instagram.

---

## 📱 Visão Geral do Produto

Diferente de um simples gerenciador de arquivos ou gravador de voz, o Priv transforma mensagens soltas em uma **memória viva**, organizada por pessoas, momentos, eventos e linhas do tempo emocionais.

### Principais Pilares:
1. **Memória, Não Armazenamento**: Cada áudio e imagem é parte de uma história com notas, contexto e pessoas associadas.
2. **Privacidade por Padrão**: Todos os conteúdos são privados por padrão com segurança multi-tenant (RLS). Sem feeds públicos ou redes sociais.
3. **Captura Rápida via ACTION_SEND**: Compartilhe qualquer áudio do WhatsApp diretamente para o Priv e continue seu dia — organize agora ou depois.
4. **Offline-First & Sincronização em Nuvem**: Funciona 100% offline via Room SQLite e sincroniza em segundo plano com o Supabase usando WorkManager.

---

## 🛠️ Tecnologias & Arquitetura

- **Linguagem**: Kotlin 1.9.24
- **UI & Design**: Jetpack Compose com Material Design 3 (Identidade Visual Expressiva Priv)
- **Arquitetura**: Clean Architecture + MVVM + Repository Pattern
- **Banco de Dados Local**: Room Database 2.6.1 com KSP
- **Processamento em 2º Plano**: WorkManager 2.9.0
- **Reprodução de Mídia**: Media3 / AudioPlayerManager customizado com barra de progresso e visualizador de ondas
- **Nuvem & Backend**: Supabase (PostgreSQL com RLS, Auth, Storage API)
- **Rede & HTTP**: OkHttp 4.12.0 / Retrofit 2.9.0
- **Testes**: JUnit 4 + Robolectric 4.11.1 (Testes locais de JVM e isolamento RLS)

---

## 📂 Índice da Documentação do Projeto

Toda a documentação técnica, arquitetural e operacional do projeto está disponível nos links abaixo:

| Documento | Descrição |
| :--- | :--- |
| [**CODEX_HANDOFF.md**](CODEX_HANDOFF.md) | **Guia Mestre de Handoff para continuidade no Codex** |
| [**HANDOFF_PRIV.md**](HANDOFF_PRIV.md) | Visão geral técnica, transferência de conhecimento e stack |
| [**CURRENT_STATUS.md**](CURRENT_STATUS.md) | Auditoria completa e status detalhado de cada módulo |
| [**ARCHITECTURE.md**](ARCHITECTURE.md) | Arquitetura do sistema, fluxos de dados, MVVM e Sync Engine |
| [**DATABASE.md**](DATABASE.md) | Esquema do banco de dados Room SQLite, entidades, DAOs e migrações |
| [**SUPABASE_SETUP.md**](SUPABASE_SETUP.md) | Guia de configuração do Supabase, tabelas, RLS e Storage Buckets |
| [**AUTHENTICATION.md**](AUTHENTICATION.md) | Fluxos de autenticação, sessão do usuário, tokens e isolamento |
| [**GOOGLE_LOGIN_SETUP.md**](GOOGLE_LOGIN_SETUP.md) | Configuração de login social e OAuth via Google / Credentials Manager |
| [**BACKUP_AND_RESTORE.md**](BACKUP_AND_RESTORE.md) | Estratégia de backup local, restauração de conta e mídias |
| [**BUILD_AND_TEST.md**](BUILD_AND_TEST.md) | Instruções de compilação, execução de testes Robolectric e Lint |
| [**PROJECT_STRUCTURE.md**](PROJECT_STRUCTURE.md) | Mapeamento completo do diretório do projeto e pacotes |
| [**ENVIRONMENT_VARIABLES.md**](ENVIRONMENT_VARIABLES.md) | Mapeamento de variáveis de ambiente (`.env`) e Secrets Plugin |
| [**KNOWN_LIMITATIONS.md**](KNOWN_LIMITATIONS.md) | Débitos técnicos, limitações atuais e nuances conhecidas |
| [**ROADMAP.md**](ROADMAP.md) | Roadmap de evolução do produto do MVP às versões futuras |
| [**CHANGELOG.md**](CHANGELOG.md) | Histórico de alterações e marcos de desenvolvimento |
| [**BETA_ISSUES.md**](BETA_ISSUES.md) | Relatório de testes beta, edge cases e feedbacks |
| [**PHYSICAL_BETA_TEST.md**](PHYSICAL_BETA_TEST.md) | Protocolo de testes em dispositivos físicos (Android 11–15) |
| [**SECURITY.md**](SECURITY.md) | Diretrizes de segurança, criptografia e isolamento multi-tenant |
| [**PRIVACY.md**](PRIVACY.md) | Manifesto de privacidade, retenção e direitos do usuário |
| [**LICENSE_NOTES.md**](LICENSE_NOTES.md) | Licenças de bibliotecas e dependências open-source |
| [**SIMULATION_INVENTORY.md**](SIMULATION_INVENTORY.md) | Inventário do que é real vs simulado |
| [**DEPENDENCIES.md**](DEPENDENCIES.md) | Matriz de dependências e compatibilidade de versoes |

---

## ⚡ Como Compilar e Executar

1. **Clonar e abrir o repositório no Android Studio** (versão Iguana ou superior recomendada).
2. **Configurar variáveis de ambiente**:
   - Copie `.env.example` para `.env`
   - Preencha `SUPABASE_URL` e `SUPABASE_ANON_KEY` com seus dados do Supabase.
3. **Sincronizar o Gradle**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Executar a Suíte de Testes**:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## 🔒 Segurança e Privacidade

- Nenhum dado é público por padrão.
- O aplicativo utiliza suporte a RLS (Row Level Security) no Supabase, garantindo que o token JWT do usuário isole estritamente seus dados de outros usuários.
- As mídias locais são salvas no diretório privado do aplicativo (`context.filesDir`).

---

## 📄 Licença e Uso

Este repositório contém o código-fonte original do projeto Priv. Direitos reservados conforme notas em [LICENSE_NOTES.md](LICENSE_NOTES.md).
