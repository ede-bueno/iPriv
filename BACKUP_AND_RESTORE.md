# BACKUP_AND_RESTORE — Estratégia de Backup e Restauração de Dados

## 1. Visão Geral
O Priv garante que o usuário **nunca perca suas memórias**. Para isso, o aplicativo implementa dois mecanismos de backup e restauração:
1. **Sincronização Contínua em Nuvem (Supabase)**: Backup automático em tempo real/segundo plano dos metadados e arquivos de mídia.
2. **Exportação & Importação de Backup Local (Arquivo ZIP/JSON)**: Permite gerar um arquivo comprimido contendo o banco de dados e as mídias para guardar no Google Drive, pendrive ou computador.

---

## 2. Exportação Local de Backup

A classe `MemoryExporter.kt` é responsável por criar o arquivo de exportação:
1. Lê todas as entidades da tabela `memories`, `persons`, `moments`, `collections` e `tags` do banco Room local.
2. Gera um manifesto estruturado em formato JSON (`priv_backup_manifest.json`).
3. Empacota todas as mídias salvas em `files/media/` junto com o manifesto em um arquivo ZIP comprimido.
4. Salva o arquivo no diretório público de Downloads ou envia via Android Sharesheet para o Google Drive/WhatsApp.

---

## 3. Restauração em Novo Dispositivo

Ao instalar o Priv em um novo celular, existem dois cenários de restauração:

### Cenário A: Restauração via Conta em Nuvem (Supabase)
1. O usuário faz login com seu e-mail e senha no Priv.
2. O `MetadataSyncWorker` consulta as memórias associadas ao `space_id` do usuário no Supabase.
3. As entidades são recriadas no banco de dados Room local com o status `PENDING_DOWNLOAD`.
4. O `MediaDownloadWorker` do WorkManager baixa gradualmente em segundo plano as mídias do bucket Supabase Storage e salva em `files/media/audio/`.
5. O player de áudio fica disponível para reprodução instantânea à medida que o download de cada arquivo termina.

### Cenário B: Importação do Arquivo de Backup ZIP
1. Na tela de Perfil / Configurações, o usuário seleciona **Importar Backup**.
2. Escolhe o arquivo `.zip` através do gerenciador de arquivos nativo do Android.
3. O Priv descompacta o arquivo no diretório privado temporário, valida a integridade do manifesto JSON e mescla os dados com o Room SQLite, evitando criar duplicatas de memórias que tenham o mesmo `id` ou checksum SHA-256.
