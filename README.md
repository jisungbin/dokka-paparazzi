## Dokka Paparazzi 📸

Connect Dokka with screenshot-testing libraries like 
[Paparazzi](https://github.com/cashapp/paparazzi), 
[Roborazzi](https://github.com/takahirom/roborazzi), and 
[Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)!

![image](https://github.com/jisungbin/dokka-paparazzi/assets/40740128/dc4f5961-d85d-4563-aad2-668904a3a95c)

If you're building a UI-centric library like a design system, you can use this plugin to generate developer 
experience-friendly documentation. This way, developers can see what this component looks like right away!

### Usage

1. [Required] Provide a snapshot directory.

```kotlin
// module's gradle.build.kts
dokkaPaparazzi {
  snapshotDir = projectDir.resolve("snapshots")
}
```
 
2. [Optional] Provide a snapshot options in KDoc.

```kotlin
// A Composable function that could be documented with Dokka.
/** 
 * @snapshotname mysnapshot,scaled
 * @snapshotsize 100,300
 */
@Composable fun MySnapshot() = Unit
```

- `@snapshotname`: The name of the snapshot to look for in the `snapshotDir` provided in step 1. 
  Multiple values may be provided, separated by "," and the snapshot file containing all given 
  names will be displayed. If not provided, the first snapshot file containing the name of the 
  composable will be displayed.
- `@snapshotsize`: The width, height of the snapshot as it will be displayed in Dokka. 
  This order must be respected, and if a value count other than two is received, this property 
  will be ignored. If no value is provided, the size of the original snapshot file is used; 
  if a value is provided, the original snapshot file is not affected, only the displayed size is changed.

Currently, only composable functions are supported, but if you need to do this for regular functions, 
please share your scenario via an new issue.

### Download

This is not yet published but will be published in the Central Portal soon.

```kotlin
plugins { 
  id("org.jetbrains.dokka") // The Dokka plugin must be applied first.
  id("land.sungbin.dokka-paparazzi") version "[version]"
}
```
