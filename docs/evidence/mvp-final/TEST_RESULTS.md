# AUDITORIA E EVIDÊNCIAS DE TESTES — MVP FINAL

## Execução dos Testes Unitários e de Isolamento RLS

- **Comando executado**: `gradle :app:testDebugUnitTest`
- **Ferramenta de Execução**: JUnit 4 com Robolectric Test Runner
- **Classe de Teste Principal**: `com.example.priv.SyncAndIsolationTest`
- **Resultado**: `BUILD SUCCESSFUL` (33 actionable tasks: 7 from cache, 26 up-to-date)

---

## Detalhamento das Etapas Validadas no Teste

1. **Conta A - Autenticação & Inicialização de Espaço**:
   - Criação de usuário e token de acesso.
   - Atribuição automática do espaço pessoal `space_personal_{userId}` com papel `OWNER`.

2. **Gravação Offline e Checksum SHA-256**:
   - Gravação local da memória em estado `LOCAL_ONLY`.
   - Cálculo e validação do checksum SHA-256 da mídia em bytes.

3. **Upload de Metadados e Mídia (Supabase)**:
   - Sincronização via PostgREST na tabela `memories` e `memory_attachments`.
   - Upload do arquivo de áudio no bucket privado `memories` no caminho `spaces/{spaceId}/memories/{memoryId}/{attachmentId}.ogg`.
   - Atualização do status local para `SYNCED` e `AVAILABLE_REMOTE`.

4. **Restauração em Instalação Limpa**:
   - Busca remota de metadados por `space_id`.
   - Reconstrução das entidades Room.
   - Download da mídia via Storage API e verificação de integridade comparando o checksum SHA-256 pré e pós-restauração.

5. **Isolamento de Segurança (Conta B x Conta A)**:
   - Criação e autenticação da Conta B.
   - Tentativa de consulta da biblioteca da Conta A usando o token da Conta B -> Bloqueado por RLS (retorno vazio / 403 Forbidden).
   - Tentativa de download do áudio da Conta A usando o token da Conta B -> Bloqueado pela Storage Security Policy.
