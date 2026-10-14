# SUPABASE_SETUP — Guia de Configuração do Backend Supabase

## 1. Visão Geral
O Priv utiliza o **Supabase** como backend remoto para autenticação de usuários (Supabase Auth), persistência de metadados em nuvem (PostgreSQL) e armazenamento remoto de mídias criptografadas/protegidas (Supabase Storage).

---

## 2. Configuração Passo a Passo no Supabase Dashboard

1. **Criar um Projeto Supabase**:
   - Acesse [https://database.new](https://database.new) e crie um novo projeto.
   - Guarde a **URL da API** (`https://xyz.supabase.co`) e a **Chave Anon pública** (`eyJ...`).

2. **Executar as Migrações SQL**:
   - No menu lateral do Supabase, acesse **SQL Editor**.
   - Abra o arquivo `app/supabase_migrations_v1.sql` localizado no repositório do app.
   - Cole o conteúdo no SQL Editor e clique em **Run**.
   - Isso criará automaticamente as tabelas `spaces`, `memories`, `persons`, `moments`, `collections`, `memory_attachments` e ativará as políticas **Row Level Security (RLS)**.

3. **Configurar o Bucket de Armazenamento (Storage)**:
   - No Supabase Dashboard, acesse **Storage**.
   - Verifique se o bucket `memories` foi criado com visibilidade **Privada** (não público).
   - O script SQL configura as políticas de storage garantindo que uploads sigam a estrutura `spaces/{spaceId}/memories/{memoryId}/...` e apenas membros do espaço correspondente leiam os arquivos.

4. **Configurar Autenticação (Auth)**:
   - No menu **Authentication > Settings**, habilite o provedor **Email/Password**.
   - Se desejar habilite o **Google OAuth** (consulte [GOOGLE_LOGIN_SETUP.md](GOOGLE_LOGIN_SETUP.md)).

---

## 3. Configuração de Variáveis no App Android

Crie um arquivo `.env` na raiz do projeto Android contendo:

```env
SUPABASE_URL=https://sua-instancia.supabase.co
SUPABASE_ANON_KEY=sua-chave-anonima-aqui
```

No `app/build.gradle.kts`, o **Secrets Gradle Plugin** lê estas variáveis e disponibiliza no código como:
`BuildConfig.SUPABASE_URL` e `BuildConfig.SUPABASE_ANON_KEY`.

---

## 4. Teste de Validação de RLS
Para testar se a segurança multi-tenant está funcionando:
1. Cadastre o Usuário A e crie uma memória.
2. Faça logout e entre como Usuário B.
3. O Usuário B **não verá** nenhuma memória do Usuário A, e requisições HTTP diretas para metadados ou storage do Usuário A retornarão HTTP `403 Forbidden` ou lista vazia.
