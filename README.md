# intellij-plugins

Personal IntelliJ plugins.

## find-instance-creations

Adds one action, **Find Instance Creations** (id `FindInstanceCreations`), which finds usages of the
Scala class under the caret restricted to instance creation, in production sources.

It exists because the equivalent built-in route cannot be automated. The Scala plugin's Find Usages
settings dialog has an "Instance creation only" checkbox, but `ScalaTypeDefinitionUsagesDialog`
clears it on every open, deliberately:

```scala
//this mode should be available only from dialog, so unselect it every time
myCbOnlyNewInstances.setSelected(false)
```

So the box has to be ticked by hand on every search, and no persisted setting can change that.

### What it searches

The flag's only consumer is `ScalaFindUsagesHandlerBase.getPrimaryElements()`, which swaps the search
elements for `clazz.constructors ++ applyFactoryMethods(clazz)`. The action sets the flag just long
enough to collect those elements, restores it, and runs the search with its own options, so nothing
leaks into a subsequent plain Find Usages. Matches:

- `new Foo(...)` - all constructors.
- `Foo(...)` on a case class, via the synthetic `apply`.
- not `Foo(...)` where the companion has a hand-written `apply` - the plugin's filter keeps only
  synthetic ones.

Scope is `GlobalSearchScopesCore.projectProductionScope`, set on the action's own options object, so
the IDE-wide default Find Usages scope is left alone.

### Binding it

```vim
nmap <leader>N <Action>(FindInstanceCreations)
```

### Building

Gradle 9.7.1 via the checked-in wrapper, toolchain Java 25 - IntelliJ 2026.2 ships class files at
major version 69, so anything earlier cannot compile against it.

```
./gradlew :find-instance-creations:runIde       # try it in a sandbox IDE
./gradlew :find-instance-creations:buildPlugin  # zip in build/distributions, install from disk
```

### Version pins to review on an IDE upgrade

`build.gradle.kts` pins `intellijIdeaUltimate("2026.2")` and `plugin("org.intellij.scala", "2026.2.19")`,
matching IU-262.10315.125 and Scala 2026.2.19.

`ScalaFindUsagesConfiguration`, `ScalaTypeDefinitionFindUsagesOptions` and the flag read inside
`getPrimaryElements()` are Scala plugin internals with no compatibility promise. A break shows up as
the action finding nothing rather than as an error, so it is worth a quick check after each upgrade.
