# SIMULATION_INVENTORY — Inventário do que é Real vs Simulado

## 1. Visão Geral
Este documento apresenta uma auditoria rigorosa de cada componente do sistema, especificando o que está operando com **código real de produção** e o que foi deixado como **estrutura simulada / mock de suporte**.

---

## 2. Tabela Detalhada de Funcionalidades e Implementação

| Módulo / Funcionalidade | Estado da Implementação | Nível de Integração |
| :--- | :--- | :--- |
| **Interface Jetpack Compose / Material 3** | 🟢 **100% Real** | Código nativo Compose com layouts responsivos, suporte a tema claro/escuro e componentes customizados |
| **Captura via ACTION_SEND (WhatsApp)** | 🟢 **100% Real** | Interceptação de intent no AndroidManifest, cópia física do arquivo e salvamento no Room |
| **Banco de Dados Room SQLite** | 🟢 **100% Real** | Tabelas com UUID, relacionamentos, DAOs fortemente tipados e transações |
| **Edição e Lixeira com Confirmação** | 🟢 **100% Real** | Transições de estado `is_in_trash`, restauração e exclusão física permanente com alertas |
| **Player de Áudio & Ondas Sonoras** | 🟢 **100% Real** | Utiliza `MediaPlayer` nativo, barras de visualizador de amplitude e formatação de tempo real |
| **Importação de Contatos do Celular** | 🟢 **100% Real** | Utiliza `ContactsImporter` lendo a `ContactsContract` do Android com permissões |
| **Autenticação Supabase Auth** | 🟢 **100% Real** | Endpoints HTTP `/auth/v1` executando cadastro, login, refresh token e logout |
| **Sincronização em Nuvem (WorkManager)** | 🟢 **100% Real** | Workers executando solicitações PostgREST e Storage API em segundo plano |
| **Exportação e Backup ZIP** | 🟢 **100% Real** | Gera arquivo zip contendo o manifesto JSON e cópias dos arquivos de áudio |
| **Dados Iniciais de Demonstração (Seed)** | 🟡 **Simulado (Controlado)** | Classe `DatabaseSeed.kt` preenche dados de demonstração apenas na primeira inicialização se o banco estiver vazio |
| **Transcrição de Áudio com IA** | 🔴 **Pendente (V2)** | Estrutura de UI e campo de banco prontos; modelo Speech-to-Text não integrado no MVP |
| **Notificações Push Reais (FCM)** | 🔴 **Pendente (V2)** | Lembretes "Neste Dia" executam em agendador local, não em servidor push remoto |
