# SECURITY — Diretrizes de Segurança, Isolamento e Criptografia

## 1. Princípios de Segurança do Priv

### A. Privacidade por Padrão (Privacy by Default)
- Nenhuma memória, áudio ou imagem é compartilhada publicamente.
- O aplicativo não possui feed público, diretório de busca de usuários ou descoberta social.

### B. Isolamento Multi-Tenant via Row Level Security (RLS)
- Toda e qualquer requisição enviada do cliente para o banco de dados remoto Supabase utiliza a chave anon pública associada ao token de autenticação JWT do usuário logado (`Authorization: Bearer <user_jwt>`).
- O PostgreSQL impõe políticas estritas de RLS ativadas nas tabelas em `supabase_migrations_v1.sql`:
  ```sql
  CREATE POLICY "Users can only access memories of their spaces"
  ON public.memories FOR ALL
  USING (
    space_id IN (
      SELECT id FROM public.spaces WHERE owner_user_id = auth.uid()
    )
  );
  ```
- Isso garante que a tentativa de leitura ou alteração maliciosa de dados de outro usuário resulte em falha no nível do próprio banco de dados em nuvem.

---

## 2. Proteção de Dados Locais no Android

### A. Armazenamento Privado de Arquivos
- Os arquivos de áudio e imagem são salvos no armazenamento interno privado do aplicativo (`context.filesDir/media/`).
- Outros aplicativos instalados no dispositivo não possuem permissão do sistema operacional Android para ler ou varrer essa pasta.

### B. Proteção por Biometria / PIN Local
- Através do `SecurityManager.kt`, o usuário pode exigir autenticação por impressão digital / reconhecimento facial (via `BiometricPrompt`) ou PIN numérico sempre que o aplicativo for aberto.

### C. Proteção de Chaves de API
- Chaves de API e URLs do Supabase são injetadas em tempo de compilação via `BuildConfig` e Secrets Gradle Plugin, não devendo ser codificadas diretamente nas classes Kotlin.
