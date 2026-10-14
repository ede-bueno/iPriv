# GOOGLE_LOGIN_SETUP — Configuração do Login com Google no Priv

## 1. Visão Geral
O Priv inclui integração pronta via `GoogleAuthManager.kt` para autenticação de usuários via **Google Sign-In / Credentials Manager API** e **Supabase OAuth Provider**.

---

## 2. Passo a Passo para Ativação no Google Cloud Console

1. **Google Cloud Console**:
   - Acesse o [Google Cloud Console](https://console.cloud.google.com/).
   - Crie um novo projeto ou selecione um existente.
   - Acesse **APIs & Services > Credentials**.

2. **OAuth 2.0 Web Client ID (Para o Supabase)**:
   - Crie uma credencial de **OAuth Client ID** do tipo **Web Application**.
   - Adicione nas URIs de Redirecionamento Autorizadas:
     `https://<seu-projeto-supabase>.supabase.co/auth/v1/callback`
   - Guarde o **Client ID** e o **Client Secret** gerados.

3. **OAuth 2.0 Android Client ID**:
   - Crie outra credencial de **OAuth Client ID** do tipo **Android**.
   - Informe o nome do pacote: `com.aistudio.privmemoria.kxmpzq` (ou seu pacote final ex: `br.com.priv.app`).
   - Informe a impressão digital SHA-1 do certificado de assinatura de debug e release (`keytool -list -v -keystore ~/.android/debug.keystore`).

---

## 3. Configuração no Supabase Dashboard

1. No Supabase Dashboard, acesse **Authentication > Providers > Google**.
2. Ative o provedor Google.
3. Cole o **Client ID (Web)** e o **Client Secret (Web)** gerados no Google Cloud Console.
4. Salve as alterações.

---

## 4. Integração no App Android
No código Android, a classe `GoogleAuthManager.kt` utiliza o **Credential Manager API** (`androidx.credentials`) do Android para apresentar o prompt nativo do Google de "Continuar como...". Ao selecionar a conta, o idToken retornado do Google é enviado para o Supabase Auth endpoint `/auth/v1/token?grant_type=id_token`.
