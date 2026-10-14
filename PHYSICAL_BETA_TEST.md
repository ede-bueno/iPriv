# PHYSICAL_BETA_TEST — Protocolo de Testes em Dispositivos Físicos

## 1. Objetivo do Teste
Validar a usabilidade, captura via compartilhamento nativo (`ACTION_SEND`), reprodução de áudio, consumo de bateria e sincronização em segundo plano em smartphones Android reais de diferentes fabricantes e versões de sistema (Android 11 a Android 15).

---

## 2. Aparelhos e Cobertura Recomendada
1. **Samsung Galaxy** (One UI, Android 12 e 14) — Testar permissões de fundo e gerenciamento de energia agressivo.
2. **Xiaomi / Redmi** (MIUI / HyperOS, Android 13) — Testar autostart do WorkManager e copia de áudio do WhatsApp.
3. **Motorola Moto G** (Android 11 e 13) — Testar desempenho em dispositivos com 3GB/4GB RAM.
4. **Google Pixel** (Android 14 / 15) — Testar navegação por gestos e visualização em Material You.

---

## 3. Roteiro de Testes (Checklist de Validação)

### Teste 01: Captura de Áudio via WhatsApp
1. Abra o WhatsApp no smartphone.
2. Selecione uma mensagem de voz ou áudio recebido em uma conversa.
3. Toque nos três pontos > **Compartilhar** (ou toque no ícone de compartilhamento).
4. Selecione o aplicativo **Priv** na lista de aplicativos.
5. **Resultado Esperado**: O Priv abre a tela de Captura Rápida (`QuickCaptureSheet`), permitindo selecionar a pessoa, digitar uma nota e salvar. O áudio deve ser copiado e ficar disponível para reprodução instantânea na Biblioteca.

### Teste 02: Reprodução de Áudio e Formatos
1. Vá até a aba **Biblioteca**.
2. Toque no botão Play do cartão da memória recém-salva.
3. **Resultado Esperado**: O áudio inicia imediatamente, a barra de progresso e as ondas de frequência se movimentam suavemente, e o tempo atual é formatado corretamente em minutos/segundos.

### Teste 03: Edição e Envio para a Lixeira com Confirmação
1. Em um cartão de memória, toque no menu de opções ou no botão de editar.
2. Altere o título e adicione uma nota. Salve.
3. Toque no ícone da **Lixeira**.
4. **Resultado Esperado**: É exibido o diálogo modal perguntando *"Mover para a Lixeira?"*. Ao confirmar, a memória desaparece da listagem principal e vai para o filtro de Lixeira.

### Teste 04: Restauração e Exclusão Permanente
1. Na aba **Biblioteca**, altere o filtro para **Lixeira**.
2. Toque em **Restaurar** em uma memória. Ela deve voltar para a Biblioteca.
3. Em outra memória na Lixeira, toque em **Excluir Definitivamente**.
4. **Resultado Esperado**: É exibido o diálogo modal perguntando *"Excluir definitivamente?"*. Ao confirmar, a memória e o arquivo local são removidos permanentemente.
