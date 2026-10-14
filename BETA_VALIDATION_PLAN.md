# BETA_VALIDATION_PLAN — Plano de Testes e Validação em Aparelho Físico

Este documento traz o checklist passo a passo para execução da bateria de testes em aparelhos Android físicos (versões Android 8.0 a Android 14).

---

## Roteiro de Testes Beta (Checklist Físico)

### 1. Instalação e Permissões
- [ ] Transferir o APK `app-debug.apk` para o celular físico via USB ou download direto.
- [ ] Executar o instalador do Android e permitir a instalação de fontes desconhecidas se solicitado.
- [ ] Abrir o app e validar a concessão de permissões de armazenamento/mídia e notificações.

### 2. Cadastro e Login Inicial
- [ ] Abrir a tela de autenticação.
- [ ] Criar uma nova conta com e-mail e senha.
- [ ] Confirmar no Supabase Console a criação automática do `profile` e do espaço `Meu Priv`.
- [ ] Realizar logout e login novamente com as mesmas credenciais.

### 3. Compartilhamento Real pelo WhatsApp
- [ ] Abrir uma conversa real no WhatsApp no celular.
- [ ] Selecionar uma mensagem de voz/áudio recebida.
- [ ] Tocar em **Compartilhar** (Share) e escolher o aplicativo **Priv** na Android Sharesheet.
- [ ] Confirmar que o `QuickCaptureSheet` abre com as informações do áudio pré-preenchidas.
- [ ] Associar uma pessoa (ex: "Helô") e salvar.

### 4. Importação Manual e Reprodução
- [ ] Usar o botão "+" dentro do app para selecionar um arquivo de áudio local `.mp3` ou `.m4a`.
- [ ] Tocar no card da memória criada e acionar o Player de Áudio.
- [ ] Testar os controles: Play, Pausa, Seekbar e barra de progresso.

### 5. Teste Offline e Reconexão
- [ ] Ativar o **Modo Avião** no celular (desligar Wi-Fi e dados móveis).
- [ ] Criar uma nova memória com áudio no app Priv offline.
- [ ] Verificar se a memória é gravada instantaneamente e exibida com indicador de sincronização pendente.
- [ ] Desativar o Modo Avião e conectar-se à internet.
- [ ] Verificar a execução em segundo plano do WorkManager (`MetadataSyncWorker` / `MediaUploadWorker`) e a transição para o status sincronizado.

### 6. Sobrevivência a Fechamento Forçado e Reinicialização
- [ ] Forçar a parada do aplicativo pelas Configurações do Android (**Force Close**).
- [ ] Reabrir o Priv e verificar se a sessão de usuário e os dados do Room permanecem intactos.
- [ ] Reiniciar o celular completo.
- [ ] Reabrir o app e confirmar a persistência dos dados.

### 7. Limpeza e Restauração em Aparelho Limpo
- [ ] Ir em **Configurações do Android > Aplicativos > Priv > Armazenamento > Limpar Dados / Limpar Cache**.
- [ ] Reabrir o Priv (o app inicializará limpo).
- [ ] Fazer Login com a conta criada no Passo 2.
- [ ] Confirmar que a linha do tempo, pessoas e memórias são restauradas da nuvem.
- [ ] Tocar para reproduzir o áudio restaurado e confirmar o download sob demanda funcional.

### 8. Isolamento de Dados (Duas Contas)
- [ ] Fazer logout da Conta A e criar uma Conta B no mesmo ou em outro celular.
- [ ] Confirmar que a biblioteca da Conta B inicia vazia e **nenhum dado da Conta A** é exibido.
