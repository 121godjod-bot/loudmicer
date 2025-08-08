rootProject.name = "AliucordPlugins"

// Plugins are included like this
include(
    // Remove "MyFirstCommand" if you are not using it anymore
    // Add "MicAmplifier" when you create that plugin folder
    "MicAmplifier",      // Add your new plugin here
    "MyFirstPatch"       // Keep this if you need it
)

// Adjust project directories for your plugin structure
rootProject.children.forEach {
    it.projectDir = file("ExamplePlugins/kotlin/${it.name}")
}
