# ROADMAP — Plano de Evolução do Produto Priv

## 1. Fase 1: MVP Android Nativo (Concluído ✅)
- [x] Estrutura Android Nativo Kotlin + Jetpack Compose + Material Design 3.
- [x] Captura rápida de áudio e mídias via intent nativa `ACTION_SEND` (WhatsApp/Instagram).
- [x] Banco de dados Room local com persistência offline e arquivos de mídia privados.
- [x] Player de áudio integrado com waveform, busca e formatação de tempo.
- [x] Cadastro e gerenciamento de Pessoas, Momentos, Coleções e Tags.
- [x] Edição de metadados, busca por texto e lixeira com recuperação e exclusão permanente com confirmação.
- [x] Sincronização em nuvem e isolamento RLS via Supabase Auth/PostgreSQL/Storage.
- [x] Suíte de testes unitários Robolectric e suporte a backup local ZIP.

---

## 2. Fase 2: Lançamento Beta & Polimento de Produção (Próxima Etapa no Codex)
- [ ] Alteração do `applicationId` para o namespace final de publicação (ex: `br.com.priv.app`).
- [ ] Conexão do aplicativo ao projeto Supabase de Produção real.
- [ ] Testes de carga em dispositivos físicos Android (Android 11 até Android 15).
- [ ] Implementação de Notificações Push nativas via Firebase Cloud Messaging (FCM) para lembretes de memórias ("Olha o que aconteceu neste dia há 1 ano").
- [ ] Publicação do aplicativo na Google Play Store em canal fechado de Beta.

---

## 3. Fase 3: Inteligência de Memória & Mídia Expandida (Versão 2.0)
- [ ] Transcrição automática de mensagens de voz e áudios via modelo Whisper / Gemini.
- [ ] Busca semântica e por assunto ("Encontre áudios engraçados sobre a viagem do ano passado").
- [ ] Suporte estendido a reprodução e armazenamento de vídeos.
- [ ] Geração automática de cápsulas do tempo e retrospectivas interativas multimídia.
- [ ] Proteção de cofres de memórias por biometria ou senha individual por pessoa/momento.
