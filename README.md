# intellij-plugins

Personal IntelliJ plugins.

## alex-mcguire-plugin

One plugin, so there is only ever one thing to install. Everything below lives in it.

### Find Instance Creations

Two actions, both finding usages of the Scala class under the caret restricted to instance
creation, and differing only in scope:

| Action | Id | Scope |
| --- | --- | --- |
| Find Instance Creations | `FindInstanceCreations` | whole project, tests included |
| Find Instance Creations in Production | `FindProductionInstanceCreations` | production sources only |

It exists because the equivalent built-in route cannot be automated. The Scala plugin's Find Usages
settings dialog has an "Instance creation only" checkbox, but `ScalaTypeDefinitionUsagesDialog`
clears it on every open, deliberately:

```scala
//this mode should be available only from dialog, so unselect it every time
myCbOnlyNewInstances.setSelected(false)
```

So the box has to be ticked by hand on every search, and no persisted setting can change that.

### What it searches

The action collects the search elements itself: the class's constructors plus every `apply` in its
companion whose return type is the class. Matches:

- `new Foo(...)` - all constructors.
- `Foo(...)` on a case class, via the synthetic `apply`.
- `Foo(...)` where the companion has a hand-written `apply` returning `Foo`.

That last case is deliberately wider than the Scala plugin's own "Instance creation only", whose
`applyFactoryMethods` keeps only *synthetic* applies. In a codebase that rarely writes `new`, the
built-in filter misses most instantiations.

Both set the scope on their own options object - `GlobalSearchScope.projectScope` and
`GlobalSearchScopesCore.projectProductionScope` - so the IDE-wide default Find Usages scope is left
alone. Neither searches libraries; `GlobalSearchScope.allScope` would widen the unrestricted one if
that is ever wanted.

Both rows stay in the menu at all times and grey out when the caret is not on a Scala class, rather
than disappearing.

### Binding it

```vim
nmap <leader>N <Action>(FindInstanceCreations)
nmap <leader>P <Action>(FindProductionInstanceCreations)
```

### Close Others and Pin

Id `CloseOthersAndPin`, for switching focus to a new piece of work:
closes every open editor tab except the focused one - pinned tabs included, and across every split -
then pins the survivor.

The built-in `CloseAllEditorsButActive` leaves pinned tabs open and does not pin anything, which is
why this exists. Pinned files are unpinned first, since the pin is what protects a tab from being
closed.

It appears in `CloseEditorsGroup`, so it shows up both on the editor tab right-click menu and under
Window -> Editor Tabs.

```vim
nmap <leader>o <Action>(CloseOthersAndPin)
```

That would replace the current `<leader>o` mapping to `CloseAllEditorsButActive`.

### Building

Gradle 9.7.1 via the checked-in wrapper, toolchain Java 25 - IntelliJ 2026.2 ships class files at
major version 69, so anything earlier cannot compile against it.

```
./gradlew :alex-mcguire-plugin:runIde       # try it in a sandbox IDE
./gradlew :alex-mcguire-plugin:buildPlugin  # zip in build/distributions, install from disk
```

### Version pins to review on an IDE upgrade

`alex-mcguire-plugin/build.gradle.kts` pins `intellijIdeaUltimate("262.10315.125")` and
`plugin("org.intellij.scala", "2026.2.19")`. Pin the IDE by **build number**, not by `"2026.2"`: that
resolves to the initial 2026.2 release, IU-262.8665.258, while Scala 2026.2.19 declares
`since-build="262.10315"` and is refused as incompatible in the sandbox. The two pins have to move
together. `local("/Applications/IntelliJ IDEA.app")` in place of `intellijIdeaUltimate(...)` is the
no-download alternative, at the cost of a machine-specific path.

`ScalaFindUsagesConfiguration`, `ScalaTypeDefinitionFindUsagesOptions` and the flag read inside
`getPrimaryElements()` are Scala plugin internals with no compatibility promise. A break shows up as
the action finding nothing rather than as an error, so it is worth a quick check after each upgrade.
