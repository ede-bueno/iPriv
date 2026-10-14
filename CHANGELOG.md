# CHANGELOG — Histórico de Alterações do Projeto Priv

Todas as alterações notáveis do projeto Priv no Google AI Studio são registradas neste documento.

---

## [1.0.0-handoff] - 2026-08-02

### Adicionado
- **Modal de Confirmação de Exclusão**: Implementado alerta de confirmação ao mover memórias para a Lixeira e ao efetuar exclusão permanente da Lixeira.
- **Edição Completa de Memória**: Diálogo interativo para atualização de título, notas e tags de qualquer áudio ou mídia.
- **Lixeira & Restauração**: Suporte a filtro de Lixeira na Biblioteca, com opções de restaurar e deletar permanentemente.
- **Captura Rápida via ACTION_SEND**: Interceptação e cópia instantânea de áudios compartilhados pelo WhatsApp com criação de entidade no Room.
- **Player de Áudio Customizado**: `AudioPlayerManager` com barras de onda sonoras ativas, reprodução e seek bar.
- **Múltiplos Módulos UI**: Telas Início, Pessoas e Grupos, Biblioteca, Momentos e Coleções, e Perfil do Usuário.
- **Sincronização Supabase**: Integração com Supabase PostgREST, Auth e Storage Bucket privado via WorkManager.
- **Políticas RLS**: Arquivo `supabase_migrations_v1.sql` contendo esquema de tabelas e Row Level Security para isolamento multi-tenant por `space_id`.
- **Importador de Contatos Nativo**: Classe `ContactsImporter.kt` integrando lista de contatos do Android às Pessoas no app.
- **Exportação e Backup**: Classe `MemoryExporter.kt` para geração de arquivos ZIP de backup local.
- **Testes Unitários Robolectric**: Suíte `SyncAndIsolationTest.kt` validando isolamento RLS, ciclo de vida da Lixeira e fluxo offline.
- **Documentação Completa de Handoff**: Criação de 23 arquivos de documentação técnica para migração transparente para o Codex.

---

## [0.1.0-alpha] - Inicialização
- Criação da estrutura base Android Kotlin, Jetpack Compose e Room Database v1.
