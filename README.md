<div align="center">

<img src="https://raw.githubusercontent.com/Inertia4J/inertia4j/refs/heads/assets/logo.webp" width="240" alt="Inertia4J"/>

<h2>Inertia4J — fork <code>jakjr</code></h2>

<p>Adapter server-side do <a href="https://inertiajs.com/">Inertia.js</a> pra JVM, com o protocolo
<strong>v2/v3 completo</strong> e suporte a <strong>Quarkus</strong>.</p>

<p>
<img alt="version" src="https://img.shields.io/badge/inertia4j--core-1.5.0-blue">
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
| **Quarkus** | 🎯 foco ativo deste fork | [`inertia4j.quarkus`](/inertia4j.quarkus) — CDI (`Inertia`, `InertiaShared`, `InertiaFlash`), sessão Vert.x/Redis, mapper de validação, template renderer ciente do Quinoa |
| Spring | mantido, herdado do projeto original | [`inertia4j.spring`](/inertia4j.spring/README.md) |
| Ktor | mantido, herdado do projeto original | [`inertia4j.ktor`](/inertia4j.ktor/README.md) |

Spring e Ktor continuam no repositório e recebem de graça todas as correções de protocolo do
`inertia4j-core` (nenhuma delas é específica de framework) — mas não são o foco de desenvolvimento
ativo, que é o núcleo do protocolo e o adapter Quarkus.

`inertia4j.quarkus` fornece a infraestrutura genérica (o bean `Inertia`, sessão, validação,
`sharedProps`, flash); o que cada app compartilha de fato — o equivalente a sobrescrever
`HandleInertiaRequests::share()` — é responsabilidade do app, tipicamente um
`ContainerRequestFilter` próprio (exemplo real:
[`InertiaSharedDataFilter`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/tarefas-inertia/src/main/java/io/github/inertia4j/quarkus/InertiaSharedDataFilter.java)
em [`quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab), o projeto que usa este
módulo como dependência e serve de demo ponta a ponta).

## Instalação

Este fork **não é publicado no Maven Central** — publique localmente e aponte seu projeto pras
mesmas coordenadas. Pra um app Quarkus, publique os três (`core`/`spi`/`quarkus`):

**Sobre os números de versão** (2026-08-30): sem sufixo `-jakjr.N` — os três módulos usam versão
"limpa" (`1.5.0`, `1.3.0`, `1.0.0`), igual a qualquer lib publicada de verdade. Enquanto um recurso
está em desenvolvimento ativo, a versão vira `X.Y.Z-SNAPSHOT` (cada `publishToMavenLocal` sobrescreve
o `.jar` local, sem precisar bumpar nada) — só volta a um número redondo quando o trabalho é
consolidado. Isso existe pra não ter que atualizar a seção de instalação (aqui embaixo) a cada
commit — só quando uma versão de verdade sai.

```bash
./gradlew :inertia4j.core:publishToMavenLocal :inertia4j.spi:publishToMavenLocal :inertia4j.quarkus:publishToMavenLocal
```

Declare os três diretamente — `inertia4j-quarkus` traz `inertia4j-core` só em tempo de execução
(não no classpath de compilação de quem o consome), e você vai precisar de `core`/`spi` diretamente
se usar os prop types (`DeferProp`, `MergeProp`, `ScrollProp`, etc.) nos seus próprios Resources:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.inertia4j:inertia4j-quarkus:1.0.0")
    implementation("io.github.inertia4j:inertia4j-core:1.5.0")
    implementation("io.github.inertia4j:inertia4j-spi:1.3.0")
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-quarkus</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-core</artifactId>
    <version>1.5.0</version>
</dependency>
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-spi</artifactId>
    <version>1.3.0</version>
</dependency>
```

`inertia4j.quarkus` declara as dependências do Quarkus (`quarkus-arc`, `quarkus-rest`,
`quarkus-vertx`, `quarkus-hibernate-validator`, `quarkus-redis-client`,
`io.vertx:vertx-web-sstore-redis`) como `compileOnly` — seu app, que já é um app Quarkus, precisa
declará-las diretamente (na versão do BOM que seu app escolher), pra evitar duas cópias
conflitantes na classpath.

## Desenvolvimento local (editando a lib e um app consumidor ao mesmo tempo)

Se você forkar este repo (ou só quiser evoluir a lib enquanto testa contra seu próprio app
Quarkus), vai esbarrar num problema real: `quarkus:dev` **não** detecta sozinho quando um `.jar` de
dependência muda no `~/.m2` — é preciso republicar (`publishToMavenLocal`) e reiniciar o
`quarkus:dev` a cada alteração na lib
([limitação documentada do Quarkus](https://github.com/quarkusio/quarkus/issues/27242), não falta
de configuração). Isso é lento pra iterar em par com um app consumidor, especialmente quando os
dois vivem em repositórios separados como aqui.

**A solução (testada de ponta a ponta neste projeto — não é teoria)**: no `pom.xml` do app
consumidor, adicione um profile Maven opt-in que compila `inertia4j.spi`/`core`/`quarkus` direto do
código-fonte do checkout deste repo, como source roots extras do próprio módulo do app — sem
`.jar`, sem publish nenhum. Como vira a mesma unidade de compilação do app, o live reload do
`quarkus:dev` trata uma mudança na lib exatamente como mudança local, sem restart (confirmado: o
`RuntimeUpdatesProcessor` do Quarkus loga o reload normalmente).

```xml
<!-- pom.xml do app consumidor -->
<profiles>
  <profile>
    <id>local-inertia4j</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>build-helper-maven-plugin</artifactId>
          <version>3.6.1</version>
          <executions>
            <execution>
              <id>add-inertia4j-sources</id>
              <phase>generate-sources</phase>
              <goals><goal>add-source</goal></goals>
              <configuration>
                <sources>
                  <source>${project.basedir}/../inertia4j/inertia4j.spi/src/main/java</source>
                  <source>${project.basedir}/../inertia4j/inertia4j.core/src/main/java</source>
                  <source>${project.basedir}/../inertia4j/inertia4j.quarkus/src/main/java</source>
                </sources>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Ative com `./mvnw -Plocal-inertia4j quarkus:dev`. Importante: as três dependências
`io.github.inertia4j:inertia4j-{core,spi,quarkus}` **não podem estar declaradas** nesse profile (ou
em qualquer lugar ativo junto com ele) — ter a classe simultaneamente como fonte compilada e dentro
de um `.jar` no classpath é receita pra um `.jar` desatualizado silenciosamente ganhar de uma
versão mais nova. A forma mais simples de garantir isso: mova essas três dependências pra dentro de
um segundo profile (`activeByDefault=true`), mutuamente exclusivo com `local-inertia4j` — o Maven
desativa sozinho um profile `activeByDefault` assim que outro é pedido na linha de comando. Exemplo
completo, testado e funcionando, no
[`pom.xml`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/tarefas-inertia/pom.xml) do
`quarkus-inertia-lab` (profiles `published-inertia4j`/`local-inertia4j`) — copie a estrutura de lá.

Isso é específico de Maven; se seu app consumidor for Gradle, o equivalente nativo é um
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
(`includeBuild("../inertia4j")`) — o Quarkus tem suporte a live-reload de composite builds, mas
há issues abertas específicas desse caminho (conflito de classloader quando o build incluído
compartilha uma dependência com o projeto principal, entre outras) — menos testado em produção do
que a rota Maven acima.

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
