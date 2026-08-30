<div align="center">

<img src="https://raw.githubusercontent.com/Inertia4J/inertia4j/refs/heads/assets/logo.webp" width="240" alt="Inertia4J"/>

<h2>Inertia4J — fork <code>jakjr</code></h2>

<p>Adapter server-side do <a href="https://inertiajs.com/">Inertia.js</a> pra JVM, com o protocolo
<strong>v2/v3 completo</strong> e suporte a <strong>Quarkus</strong>.</p>

<p>
<img alt="version" src="https://img.shields.io/badge/inertia4j--core-1.5.0--SNAPSHOT-blue">
<img alt="license" src="https://img.shields.io/badge/license-Apache--2.0-informational">
<img alt="protocol" src="https://img.shields.io/badge/Inertia.js-v3%20protocol-9553E9?logo=inertia&logoColor=white">
<img alt="tests" src="https://img.shields.io/badge/InertiaRendererTest-70%20casos-success">
</p>

</div>

---

## Sobre este fork

Fork de [`Inertia4J/inertia4j`](https://github.com/Inertia4J/inertia4j), criado por
[@edrd-f](https://github.com/edrd-f) e [@pefcos](https://github.com/pefcos), acrescentando suporte
a Quarkus (o projeto oficial cobre Spring e Ktor) e fechando o protocolo Inertia v2/v3 por
completo. Mesma licença (Apache 2.0) e mesmo pacote (`io.github.inertia4j`) do projeto original.

## O que este fork acrescenta sobre a v1.0.4 original

A v1.0.4 original cobre o básico do protocolo (props, partial reloads, asset versioning,
redirects). Este fork acrescenta, verificado contra o código-fonte real do
`inertia-laravel`/`inertia-rails`:

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

70 casos em `InertiaRendererTest` cobrem tudo isso (`./gradlew :inertia4j.core:test`). Detalhes de
design de cada recurso estão em [`plan.md`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/plan.md)
e [`roadmap.md`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/roadmap.md), no projeto
consumidor [`quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab).

## Frameworks suportados

| Framework | Módulo |
|---|---|
| Quarkus | [`inertia4j.quarkus`](/inertia4j.quarkus) — CDI (`Inertia`, `InertiaShared`, `InertiaFlash`), sessão Vert.x/Redis, mapper de validação, template renderer ciente do Quinoa |
| Spring | [`inertia4j.spring`](/inertia4j.spring/README.md) |
| Ktor | [`inertia4j.ktor`](/inertia4j.ktor/README.md) |

`inertia4j.quarkus` fornece a infraestrutura genérica (o bean `Inertia`, sessão, validação,
`sharedProps`, flash). O que sua aplicação compartilha de fato — o equivalente a sobrescrever
`HandleInertiaRequests::share()` — é responsabilidade do app, tipicamente um
`ContainerRequestFilter` próprio (exemplo:
[`InertiaSharedDataFilter`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/tarefas-inertia/src/main/java/org/acme/InertiaSharedDataFilter.java)
em [`quarkus-inertia-lab`](https://github.com/jakjr/quarkus-inertia-lab)).

## Instalação

Este fork **não é publicado no Maven Central** — publique localmente e aponte seu projeto pras
mesmas coordenadas. Pra um app Quarkus, publique os três (`core`/`spi`/`quarkus`):

```bash
./gradlew :inertia4j.core:publishToMavenLocal :inertia4j.spi:publishToMavenLocal :inertia4j.quarkus:publishToMavenLocal
```

Declare os três diretamente — `inertia4j-quarkus` traz `inertia4j-core` só em tempo de execução
(não no classpath de compilação de quem o consome), e você vai precisar de `core`/`spi` diretamente
se usar os prop types (`DeferProp`, `MergeProp`, `ScrollProp`, etc.) nos seus próprios Resources:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.inertia4j:inertia4j-quarkus:1.0.0-SNAPSHOT")
    implementation("io.github.inertia4j:inertia4j-core:1.5.0-SNAPSHOT")
    implementation("io.github.inertia4j:inertia4j-spi:1.3.0-SNAPSHOT")
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-quarkus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-core</artifactId>
    <version>1.5.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.inertia4j</groupId>
    <artifactId>inertia4j-spi</artifactId>
    <version>1.3.0-SNAPSHOT</version>
</dependency>
```

Uma versão terminada em `-SNAPSHOT` está em desenvolvimento ativo e pode mudar de comportamento
entre publicações; prefira um número sem sufixo (release) quando disponível.

`inertia4j.quarkus` declara as dependências do Quarkus (`quarkus-arc`, `quarkus-rest`,
`quarkus-vertx`, `quarkus-hibernate-validator`, `quarkus-redis-client`,
`io.vertx:vertx-web-sstore-redis`) como `compileOnly` — seu app, que já é um app Quarkus, precisa
declará-las diretamente (na versão do BOM que seu app escolher), pra evitar duas cópias
conflitantes na classpath.

## Desenvolvimento local (editando a lib e um app consumidor ao mesmo tempo)

`quarkus:dev` não detecta sozinho quando um `.jar` de dependência muda no `~/.m2` — é preciso
republicar (`publishToMavenLocal`) e reiniciar o `quarkus:dev` a cada alteração na lib
([limitação documentada do Quarkus](https://github.com/quarkusio/quarkus/issues/27242)).

Alternativa: no `pom.xml` do app consumidor, um profile Maven opt-in compilando
`inertia4j.spi`/`core`/`quarkus` direto do código-fonte deste repo, como source roots extras do
próprio módulo do app — sem `.jar`, sem publish. Como vira a mesma unidade de compilação do app, o
live reload do `quarkus:dev` trata uma mudança na lib como mudança local, sem restart.

```xml
<!-- pom.xml do app consumidor -->
<profiles>
  <profile>
    <id>local-inertia4j</id>
    <activation>
      <property><name>inertia4j.local</name></property>
    </activation>
    <dependencies>
      <!-- inertia4j.spi traz o jspecify como `api`; sem o jar do spi aqui, ele precisa ser
           declarado, senão as fontes de spi/core não compilam (importam org.jspecify.annotations). -->
      <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
      </dependency>
    </dependencies>
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

Ative com `./mvnw -Dinertia4j.local quarkus:dev`. As três dependências
`io.github.inertia4j:inertia4j-{core,spi,quarkus}` **não podem estar declaradas** nesse profile (ou
em qualquer lugar ativo junto com ele) — ter a classe simultaneamente como fonte compilada e dentro
de um `.jar` no classpath arrisca um `.jar` desatualizado ganhar de uma versão mais nova. Pra
garantir isso, mova essas três dependências pra dentro de um segundo profile mutuamente exclusivo,
ativado pela *ausência* da mesma propriedade:

```xml
<profile>
  <id>published-inertia4j</id>
  <activation>
    <property><name>!inertia4j.local</name></property>
  </activation>
  <dependencies><!-- inertia4j-core/spi/quarkus aqui --></dependencies>
</profile>
```

**Não use `activeByDefault` pra isso**: o Maven desativa um profile `activeByDefault` assim que
qualquer outro profile do mesmo POM ativa — por `-P` *ou* por propriedade. Num projeto Quarkus isso
inclui o profile `native` gerado pelo arquétipo, que ativa em `-Dnative`: as dependências do
Inertia4J somem justo do `./mvnw package -Dnative` e o build morre com `package
io.github.inertia4j.core.props does not exist`. Ativação por propriedade negada não tem esse
acoplamento.

Pelo mesmo motivo o gatilho é `-Dinertia4j.local` e não `-Plocal-inertia4j`: é a propriedade que
desliga o outro profile, então `-P` sozinho ativaria os dois. Vale uma regra do
`maven-enforcer-plugin` (`requireProperty` em `inertia4j.local`) dentro do profile local pra
transformar essa invocação errada num erro explicado em vez de um `.jar` velho ganhando em
silêncio. Exemplo completo no
[`pom.xml`](https://github.com/jakjr/quarkus-inertia-lab/blob/main/tarefas-inertia/pom.xml) do
`quarkus-inertia-lab` (profiles `published-inertia4j`/`local-inertia4j`).

Pra um app consumidor Gradle, o equivalente é um
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
(`includeBuild("../inertia4j")`). O Quarkus suporta live-reload de composite builds, mas há issues
abertas específicas desse caminho, incluindo conflito de classloader quando o build incluído
compartilha uma dependência com o projeto principal.

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
