# BUILD_AND_TEST — Guia de Compilação, Testes e Linting

## 1. Ambientes & Pré-requisitos
- **Android Studio**: Iguana (2023.2.1) ou superior.
- **JDK**: OpenJDK 17 (configurado como Gradle JDK no Android Studio).
- **Android SDK**: Compile SDK 34, Min SDK 24, Target SDK 34.
- **Gradle**: 8.4+ com Kotlin DSL (`build.gradle.kts`).

---

## 2. Comandos do Gradle

### A. Sincronizar e Compilar Projeto
Para compilar o APK de desenvolvimento (Debug):
```bash
./gradlew assembleDebug
```
*(No ambiente AI Studio, o atalho de validação é a ferramenta `compile_applet`)*

### B. Compilar APK de Produção (Release)
```bash
./gradlew assembleRelease
```

### C. Executar Suíte de Testes Unitários (Robolectric)
Os testes de unidade e isolamento RLS não exigem um emulador e executam diretamente na JVM local:
```bash
./gradlew :app:testDebugUnitTest
```
Os relatórios de teste serão gerados em: `app/build/reports/tests/testDebugUnitTest/index.html`.

### D. Executar Análise de Linting
```bash
./gradlew lint
```

---

## 3. Estrutura dos Testes Unitários

O arquivo `SyncAndIsolationTest.kt` localizado em `app/src/test/java/com/example/priv/` valida:
1. **Fluxo de Captura Local**: Verificação de que uma memória gravada no Room gera status `LOCAL_ONLY` e registro na fila `sync_queue`.
2. **Sincronização com Supabase**: Validação de montagem do payload JSON e envio ao PostgREST.
3. **Isolamento de Segurança (RLS)**: Teste automatizado simulando a tentativa da Conta B em acessar memórias da Conta A, confirmando bloqueio ou lista vazia.
4. **Lixeira e Exclusão Permanente**: Validação das transições de estado de `is_in_trash = true` para remoção definitiva.
