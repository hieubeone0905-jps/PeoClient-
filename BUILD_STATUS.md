Build status
============

Mapping configuration fix
--------------------------

The previous build used the raw Fabric Intermediary artifact directly as Loom mappings. Loom 1.10.5 requires a mapping set containing a `named` namespace during Minecraft setup, so GitHub Actions stopped at:

    Could not find 'named' mapping position!

This version keeps PeoClient's existing Intermediary-named source code unchanged and generates a tiny-v2 mapping jar during Gradle configuration with these namespaces:

    official -> intermediary -> named

The `named` column is an identity copy of `intermediary`. This is intentional: the source already uses `class_*`, `method_*`, and `field_*`, so no source rewrite is needed.

Nuker impact
------------

No Nuker algorithm or Nuker settings were changed by this mapping fix. It only changes how Loom resolves Minecraft names during compilation.

The existing Nuker source therefore remains intact.
