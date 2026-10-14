# AUTHENTICATION — Autenticação e Gestão de Sessão no Priv

## 1. Visão Geral da Autenticação
O Priv suporta autenticação via **Supabase Auth** e gerenciamento de conta local. A autenticação garante a criação e isolamento do `Space` (Espaço de Memórias) do usuário.

---

## 2. Fluxo de Sessão e Tokens

1. **Cadastro / Login**:
   - O usuário informa e-mail e senha no formulário ou utiliza Login Social (Google).
   - O `SupabaseClientManager` realiza a requisição POST para `/auth/v1/signup` ou `/auth/v1/token?grant_type=password`.
   - A resposta retorna um token JWT de acesso (`access_token`), um token de renovação (`refresh_token`) e o ID do usuário (`user_id`).

2. **Persistência Segura da Sessão**:
   - Os tokens e metadados de sessão são salvos localmente via **EncryptedSharedPreferences / DataStore**.
   - Em toda reinicialização do app, a sessão ativa é recuperada e o estado global de autenticação é restaurado.

3. **Injeção de Token em Requisições**:
   - As chamadas HTTP enviadas ao PostgREST e Storage usam o cabeçalho:
     `Authorization: Bearer <access_token>`
   - O PostgreSQL do Supabase valida a assinatura do token e extrai o `auth.uid()`, aplicando automaticamente as políticas de Row Level Security (RLS).

4. **Biometria / PIN de Proteção Local (Opcional)**:
   - Em `SecurityManager.kt`, o aplicativo possui suporte a bloqueio local via **BiometricPrompt** do Android ou PIN de 4 dígitos.
   - Isso impede que pessoas com acesso físico ao telefone destravado abram o aplicativo sem a biometria do proprietário.

---

## 3. Logout e Limpeza de Sessão
Ao solicitar o logout:
- O `SupabaseClientManager` revoga o token remoto se online.
- Os tokens salvos no DataStore local são apagados.
- Os dados em cache no banco Room pertencentes ao usuário são desvinculados do estado ativo da UI.
