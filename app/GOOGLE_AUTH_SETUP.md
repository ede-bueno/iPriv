# Guia de Configuração - Google Login & Supabase Auth (Priv Android)

Status Atual do Código: **IMPLEMENTADO, MAS AGUARDANDO CONFIGURAÇÃO EXTERNA**

Este documento detalha os passos exatos necessários para ativar o Google Login real (Credential Manager + Supabase Auth) no aplicativo Android Priv.

---

## 1. Identificadores do Aplicativo Android

* **Package Name (Application ID):** `com.aistudio.privmemoria.kxmpzq`
* **Min SDK:** `24`
* **Target SDK:** `36`

### Como obter a impressão digital SHA-1 e SHA-256 da chave Debug/Release:

No terminal do projeto, execute:

```bash
# Para a chave Debug padrão do Android
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Anote as chaves de impressão digital geradas (exemplo):
* **SHA-1:** `AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:11:22:33:44`
* **SHA-256:** `00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:11:22:33:44`

---

## 2. Passo a Passo no Google Cloud Console

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/).
2. Crie ou selecione o projeto **Priv Mobile**.
3. Vá em **APIs e Serviços** > **Tela de permissão OAuth** (OAuth Consent Screen):
   * Escolha **External** (Externo) ou **Internal** (se Workspace).
   * Preencha o Nome do App: `Priv - Memórias de Voz`.
   * Adicione o e-mail de suporte e escopos básicos (`email`, `profile`, `openid`).
4. Vá em **APIs e Serviços** > **Credenciais**:
   * Clique em **Criar Credenciais** > **ID do cliente OAuth**:
     1. **Cliente Android**:
        * Nome: `Priv Android Debug`
        * Package name: `com.aistudio.privmemoria.kxmpzq`
        * SHA-1: Insira a chave SHA-1 obtida no passo 1.
     2. **Cliente Web (Web Client ID)**:
        * Nome: `Priv Web Backend / Supabase`
        * URIs de redirecionamento autorizados: Insira a URL do seu projeto Supabase:
          `https://<SEU-PROJETO-REF>.supabase.co/auth/v1/callback`
   * Guarde o **Client ID Web** gerado (exemplo: `65049652049-xxxxxxxxx.apps.googleusercontent.com`).

---

## 3. Passo a Passo no Painel Supabase

1. Acesse o painel do seu projeto no [Supabase Dashboard](https://supabase.com/dashboard).
2. Vá em **Authentication** > **Providers** > **Google**.
3. Ative a chave **Enable Google Provider**.
4. Preencha os campos:
   * **Client ID**: O Client ID Web do Google Cloud.
   * **Client Secret**: O Client Secret Web do Google Cloud.
5. Em **URL Configuration**, adicione o esquema de redirecionamento do app se aplicável:
   `com.aistudio.privmemoria.kxmpzq://login-callback`

---

## 4. Configuração das Variáveis de Ambiente no AI Studio e Android

O código Kotlin do aplicativo lê dinamicamente as credenciais através da classe `GoogleAuthManager` e `SupabaseClientManager`.

Adicione as chaves no painel **Secrets** do AI Studio ou arquivo `.env`:

```env
GOOGLE_WEB_CLIENT_ID=65049652049-xxxxxxxx.apps.googleusercontent.com
SUPABASE_URL=https://<SEU-PROJETO-REF>.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 5. Teste e Validação do Fluxo

1. Abra o aplicativo Priv e acesse a guia **Perfil**.
2. Toque em **Entrar / Vincular com Google**.
3. O Android chamará o `Credential Manager` nativo exibindo a folha oficial de contas salvas no dispositivo.
4. Ao selecionar a conta, o ID Token JWT será emitido pelo Google e enviado ao endpoint `grant_type=id_token` do Supabase.
5. A sessão do usuário será salva no `DataStore` e o perfil (Nome, E-mail, Foto e ID do Espaço Privado) ficará ativo no aplicativo.
