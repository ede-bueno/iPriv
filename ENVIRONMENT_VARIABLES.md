# ENVIRONMENT_VARIABLES — Variáveis de Ambiente e Injeção de Segredos

## 1. Visão Geral
O Priv utiliza o **Secrets Gradle Plugin** (`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`) para ler variáveis de ambiente do arquivo `.env` na raiz do projeto e injetá-las com segurança na classe `BuildConfig` do Android em tempo de compilação.

---

## 2. Variáveis Suportadas

| Nome da Variável | Tipo | Descrição | Exemplo de Valor |
| :--- | :--- | :--- | :--- |
| `SUPABASE_URL` | String | URL base do seu projeto Supabase | `https://xyzproject.supabase.co` |
| `SUPABASE_ANON_KEY` | String | Chave de API pública (Anon Key) do Supabase | `eyJhbGciOiJIUzI1NiIsInR5cCI6...` |
| `GOOGLE_WEB_CLIENT_ID` | String | (Opcional) Web Client ID do Google OAuth | `123456789-abc.apps.googleusercontent.com` |

---

## 3. Modelo de `.env.example`

```env
# Configurações do Supabase Backend
SUPABASE_URL=https://seu-projeto.supabase.co
SUPABASE_ANON_KEY=sua-chave-anon-publica

# Configurações de Login Social Google (Opcional)
GOOGLE_WEB_CLIENT_ID=seu-google-web-client-id.apps.googleusercontent.com
```

---

## 4. Acesso no Código Kotlin

No código Kotlin, o acesso às variáveis é feito de forma fortemente tipada:

```kotlin
val supabaseUrl = BuildConfig.SUPABASE_URL
val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
```

**Regras de Segurança**:
- O arquivo `.env` verdadeiro está incluído no `.gitignore` e **nunca** deve ser comitado em repositórios públicos.
- Sempre utilize o `.env.example` como gabarito para novos desenvolvedores na equipe.
