# DEPENDENCIES — Catálogo de Dependências e Matriz de Compatibilidade

## 1. Visão Geral
Todas as dependências do projeto Priv são gerenciadas centralidamente através do **Gradle Version Catalog** no arquivo `gradle/libs.versions.toml`.

---

## 2. Matriz de Versões Principais

| Biblioteca / Plugin | Versão Atual | Alias em Version Catalog (`libs.versions.toml`) |
| :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `8.2.2` | `agp` |
| **Kotlin Compiler** | `1.9.24` | `kotlin` |
| **Kotlin Symbol Processing (KSP)** | `1.9.24-1.0.20` | `ksp` |
| **AndroidX Core KTX** | `1.12.0` | `androidx.core.ktx` |
| **Jetpack Compose BOM** | `2024.02.02` | `androidx-compose-bom` |
| **Compose Activity** | `1.8.2` | `androidx.activity.compose` |
| **Navigation Compose** | `2.7.7` | `androidx.navigation.compose` |
| **Room Database** | `2.6.1` | `androidx.room` |
| **WorkManager** | `2.9.0` | `androidx.work.runtime.ktx` |
| **OkHttp** | `4.12.0` | `com.squareup.okhttp3.okhttp` |
| **Retrofit** | `2.9.0` | `com.squareup.retrofit2.retrofit` |
| **kotlinx.serialization** | `1.6.3` | `kotlinx-serialization-json` |
| **Secrets Gradle Plugin** | `2.0.1` | `secrets-gradle-plugin` |
| **Robolectric** | `4.11.1` | `robolectric` |
| **JUnit 4** | `4.13.2` | `junit` |

---

## 3. Diretrizes de Inclusão e Atualização de Dependências
1. **Regra de Conversão de Nomes**: Ao utilizar dependências no `build.gradle.kts`, converta chaves com hífen para notação de ponto (exemplo: `androidx-core-ktx` vira `libs.androidx.core.ktx`).
2. **Alinhamento do KSP e Kotlin**: O plugin KSP é estritamente vinculado à versão do Kotlin. Ao atualizar a versão do Kotlin, atualize o sufixo do KSP correspondente para evitar erros de compilação.
