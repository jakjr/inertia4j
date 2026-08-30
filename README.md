<div align="center">

<img src="https://raw.githubusercontent.com/Inertia4J/inertia4j/refs/heads/assets/logo.webp" width="240" alt="Inertia4J"/>

<h2>Inertia4J — fork <code>jakjr</code></h2>

<p>Adapter server-side do <a href="https://inertiajs.com/">Inertia.js</a> pra JVM, com o protocolo
<strong>v2/v3 completo</strong> e suporte a <strong>Quarkus</strong>.</p>

<p>
<img alt="version" src="https://img.shields.io/badge/inertia4j--core-1.5.0--jakjr.1-blue">
<img alt="license" src="https://img.shields.io/badge/license-Apache--2.0-informational">
<img alt="protocol" src="https://img.shields.io/badge/Inertia.js-v3%20protocol-9553E9?logo=inertia&logoColor=white">
<img alt="tests" src="https://img.shields.io/badge/InertiaRendererTest-70%20casos-success">
</p>

</div>

---

## Sobre este fork

Este é um fork independente de [`Inertia4J/inertia4j`](https://github.com/Inertia4J/inertia4j), a
biblioteca original criada por [@edrd-f](https://github.com/edrd-f) e
[@pefcos](https://github.com/pefcos) — **todo o crédito da concepção do projeto, da arquitetura
inicial e do trabalho até a v1.0.4 é deles**. Este fork nasceu pra portar o Inertia4J pra Quarkus (o
projeto oficial só cobre Spring e Ktor) e, no processo, acabou fechando um gap real de protocolo que
a versão original nunca cobriu.

**Por que um fork, e não um PR upstream**: o repositório original não recebe commit desde
`2025-07-26` (mais de um ano), a última release é `v1.0.4` (2025-05-26), há um PR aberto desde
maio/2026 sem revisão, e as próprias issues do projeto (`shared data`, `deferred props`, `prop
merging`) pedem exatamente o que este fork já implementou — sem resposta de mantenedor. Diante
disso, este fork segue como projeto independente: mesma licença (Apache 2.0), mesmo pacote
(`io.github.inertia4j`), mas desenvolvido e versionado aqui.

## O que este fork acrescenta sobre a v1.0.4 original

A v1.0.4 original já cobria o básico do protocolo (props, partial reloads, asset versioning,
redirects). O que foi fechado neste fork, **verificado linha a linha contra o código-fonte real dos
adapters de referência** (`inertia-laravel` e `inertia-rails` — não só a documentação do protocolo,
que descreve o formato JSON mas não a lógica de resolução):

| Recurso | O que é |
|---|---|
| **Deferred props** | Prop que não atrasa o carregamento inicial — o cliente busca numa 2ª requisição |
| **Merge / prepend / deep-merge props** | Cliente acrescenta ao valor que já tem, em vez de substituir |
| **Once props** | Resolvida uma vez, cacheada no cliente por página |
| **Scroll props** | Metadados de paginação pra infinite scroll |
| **Always / Optional props** | Sobrevive a qualquer partial reload / só resolvida se pedida por nome |
| **Flash messages** | Campo `page.flash`, fora de `props` — mecanismo próprio, não é `sharedProps` |
| **`sharedProps` genérico** | O equivalente a `Inertia::share()` — dado compartilhado automaticamente em toda página, anunciado pro cliente pra viabilizar *instant visits* |
| **Notação de ponto em chaves** | `"auth.user"` vira `{"auth": {"user": ...}}` — permite duas fontes independentes comporem o mesmo objeto sem se conhecerem |
| **Valores assíncronos (`CompletableFuture`)** | Sem equivalente em PHP/Ruby — deixa vários props resolverem I/O em paralelo em vez de sequencialmente |

70 casos em `InertiaRendererTest` cobrem tudo isso (`./gradlew :inertia4j.core:test`). O histórico
completo — cada decisão de design, cada divergência encontrada contra o código-fonte real, cada bug
achado em revisão independente — está documentado em
[`plan.md`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/plan.md) e
[`roadmap.md`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/roadmap.md) do projeto
consumidor, [`quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab).

## Frameworks suportados

| Framework | Status | Módulo |
|---|---|---|
| **Quarkus** | 🎯 foco ativo deste fork | adapter em [`jakjr/quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab) (`io.github.inertia4j.quarkus`) — não vive neste repo, consome `inertia4j-core` |
| Spring | mantido, herdado do projeto original | [`inertia4j.spring`](/inertia4j.spring/README.md) |
| Ktor | mantido, herdado do projeto original | [`inertia4j.ktor`](/inertia4j.ktor/README.md) |

Spring e Ktor continuam no repositório e recebem de graça todas as correções de protocolo do
`inertia4j-core` (nenhuma delas é específica de framework) — mas não são o foco de desenvolvimento
ativo, que é o núcleo do protocolo e o adapter Quarkus.

## Instalação

Este fork **não é publicado no Maven Central** — publique localmente e aponte seu projeto pras
mesmas coordenadas:

```bash
./gradlew :inertia4j.core:publishToMavenLocal :inertia4j.spi:publishToMavenLocal
```

`inertia4j-spi` (tipos como `PageObject`) precisa ser declarado à parte — `inertia4j-core` não o
expõe transitivamente:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.inertia4j:inertia4j-core:1.5.0-jakjr.1")
    implementation("io.github.inertia4j:inertia4j-spi:1.3.0-jakjr.1")
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-core</artifactId>
    <version>1.5.0-jakjr.1</version>
</dependency>
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-spi</artifactId>
    <version>1.3.0-jakjr.1</version>
</dependency>
```

## Rodando os testes

```bash
./gradlew :inertia4j.core:test :inertia4j.spring:test :inertia4j.ktor:test
```

## Documentação herdada do projeto original

- [Guia de uso avançado / como estender o Inertia4J](/docs/advanced.md)
- [Roadmap original (pré-fork, até a v1.0)](/docs/roadmap.md) — histórico; o roadmap ativo deste
  fork está em [`quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab)

## Licença e créditos

Apache License 2.0 (preservada do projeto original — ver [`LICENSE`](/LICENSE)).

Este fork existe por causa do trabalho de [@edrd-f](https://github.com/edrd-f) e
[@pefcos](https://github.com/pefcos) em [`Inertia4J/inertia4j`](https://github.com/Inertia4J/inertia4j).
Se você está procurando a versão oficial, mínima e sem as extensões de protocolo v2/v3 deste fork, é
lá.
