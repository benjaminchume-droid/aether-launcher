package com.aether.launcher.settings

import android.content.Context

/** Central persisted configuration for the Aether UI and interaction layer. */
data class AetherSettings(
    val setupComplete: Boolean = false,
    val homeMode: HomeMode = HomeMode.HOME_AND_DRAWER,
    val grid: GridSettings = GridSettings(),
    val dock: DockSettings = DockSettings(),
    val glass: GlassSettings = GlassSettings(),
    val motion: MotionSettings = MotionSettings(),
    val island: IslandSettings = IslandSettings(),
    val quickSpace: QuickSpaceSettings = QuickSpaceSettings(),
    val controlCenter: ControlCenterSettings = ControlCenterSettings(),
    val floatingWindows: FloatingWindowSettings = FloatingWindowSettings(),
    val splitScreen: SplitScreenSettings = SplitScreenSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val appearance: AppearanceSettings = AppearanceSettings()
)

enum class HomeMode { ALL_APPS, HOME_AND_DRAWER, DRAWER_ONLY }

data class GridSettings(
    val columns: Int = 4,
    val rows: Int = 6,
    val iconSize: Int = 56,
    val horizontalSpacing: Int = 12,
    val verticalSpacing: Int = 16,
    val horizontalPadding: Int = 20,
    val verticalPadding: Int = 24,
    val showLabels: Boolean = true,
    val labelSize: Int = 11,
    val folderStyle: FolderStyle = FolderStyle.GLASS,
    val folderRadius: Int = 24
)

data class DockSettings(
    val enabled: Boolean = true,
    val appCount: Int = 5,
    val radius: Int = 30,
    val height: Int = 76,
    val bottomPadding: Int = 18,
    val autoGroup: Boolean = true,
    val groupByCompany: Boolean = true,
    val groupByCategory: Boolean = true,
    val smartGroups: Boolean = true
)

data class GlassSettings(
    val opacity: Int = 82,
    val blur: Int = 28,
    val saturation: Int = 112,
    val refraction: Int = 18,
    val highlight: Int = 55,
    val depth: Int = 8,
    val border: Int = 35,
    val cornerRadius: Int = 28
)

data class MotionSettings(
    val preset: MotionPreset = MotionPreset.FLUID,
    val speed: Int = 100,
    val springStrength: Int = 420,
    val damping: Int = 32,
    val bounce: Int = 18,
    val gestureSensitivity: Int = 70
)

data class IslandSettings(
    val enabled: Boolean = true,
    val cutoutAware: Boolean = true,
    val width: Int = 96,
    val height: Int = 32,
    val radius: Int = 18,
    val topOffset: Int = 10,
    val showNotifications: Boolean = true,
    val showMedia: Boolean = true,
    val showCalls: Boolean = true,
    val showTimers: Boolean = true,
    val showRecording: Boolean = true,
    val showNavigation: Boolean = true,
    val blurSurroundingCutout: Boolean = true
)

data class QuickSpaceSettings(
    val enabled: Boolean = true,
    val edge: QuickEdge = QuickEdge.RIGHT,
    val handleThickness: Int = 5,
    val handleLength: Int = 42,
    val triggerDistance: Int = 80,
    val sensitivity: Int = 70,
    val panelWidth: Int = 88,
    val panelHeight: Int = 42,
    val notes: Boolean = true,
    val recorder: Boolean = true,
    val calculator: Boolean = true,
    val screenshot: Boolean = true,
    val clipboard: Boolean = true,
    val recentApps: Boolean = true,
    val shortcuts: Boolean = true
)

data class ControlCenterSettings(
    val enabled: Boolean = true,
    val edge: ControlEdge = ControlEdge.TOP_RIGHT,
    val columns: Int = 2,
    val tileRadius: Int = 24,
    val panelRadius: Int = 32,
    val showBrightness: Boolean = true,
    val showVolume: Boolean = true,
    val showWifi: Boolean = true,
    val showBluetooth: Boolean = true,
    val showAirplane: Boolean = true,
    val showRotation: Boolean = true,
    val showFlashlight: Boolean = true,
    val showDnd: Boolean = true
)

data class FloatingWindowSettings(
    val enabled: Boolean = true,
    val defaultWidth: Int = 72,
    val defaultHeight: Int = 58,
    val radius: Int = 26,
    val transparency: Int = 92,
    val shadow: Int = 70,
    val maxWindows: Int = 3,
    val edgeResize: Boolean = true,
    val cornerResize: Boolean = true,
    val snapWindows: Boolean = true,
    val rememberPositions: Boolean = true
)

data class SplitScreenSettings(
    val enabled: Boolean = true,
    val defaultRatio: Int = 50,
    val allowCustomRatio: Boolean = true,
    val rememberLayouts: Boolean = true,
    val snapToEdges: Boolean = true,
    val dividerThickness: Int = 4
)

data class SecuritySettings(
    val appLockEnabled: Boolean = true,
    val relockOnLeave: Boolean = true,
    val relockOnScreenOff: Boolean = true,
    val relockTimeoutSeconds: Int = 0,
    val useBiometrics: Boolean = true,
    val useDeviceCredential: Boolean = true,
    val hideLockedApps: Boolean = false
)

data class AppearanceSettings(
    val theme: AppearanceTheme = AppearanceTheme.SYSTEM,
    val iconPack: String = "System",
    val wallpaperDim: Int = 8,
    val showTime: Boolean = true,
    val showAetherLabel: Boolean = true
)

enum class FolderStyle { GLASS, SOLID, MINIMAL }
enum class MotionPreset { SOFT, BALANCED, FLUID, ELASTIC }
enum class QuickEdge { LEFT, RIGHT }
enum class ControlEdge { TOP_LEFT, TOP_RIGHT }
enum class AppearanceTheme { SYSTEM, LIGHT, DARK }

class AetherSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_settings", Context.MODE_PRIVATE)

    @Synchronized fun load(): AetherSettings = AetherSettings(
        setupComplete = prefs.getBoolean("setupComplete", false),
        homeMode = enum("homeMode", HomeMode.HOME_AND_DRAWER),
        grid = GridSettings(
            columns = prefs.getInt("grid.columns", 4), rows = prefs.getInt("grid.rows", 6),
            iconSize = prefs.getInt("grid.iconSize", 56), horizontalSpacing = prefs.getInt("grid.hSpacing", 12),
            verticalSpacing = prefs.getInt("grid.vSpacing", 16), horizontalPadding = prefs.getInt("grid.hPad", 20),
            verticalPadding = prefs.getInt("grid.vPad", 24), showLabels = prefs.getBoolean("grid.labels", true),
            labelSize = prefs.getInt("grid.labelSize", 11), folderStyle = enum("grid.folderStyle", FolderStyle.GLASS),
            folderRadius = prefs.getInt("grid.folderRadius", 24)
        ),
        dock = DockSettings(
            enabled = prefs.getBoolean("dock.enabled", true), appCount = prefs.getInt("dock.count", 5),
            radius = prefs.getInt("dock.radius", 30), height = prefs.getInt("dock.height", 76),
            bottomPadding = prefs.getInt("dock.bottom", 18), autoGroup = prefs.getBoolean("dock.autoGroup", true),
            groupByCompany = prefs.getBoolean("dock.company", true), groupByCategory = prefs.getBoolean("dock.category", true),
            smartGroups = prefs.getBoolean("dock.smartGroups", true)
        ),
        glass = GlassSettings(
            opacity = prefs.getInt("glass.opacity", 82), blur = prefs.getInt("glass.blur", 28),
            saturation = prefs.getInt("glass.saturation", 112), refraction = prefs.getInt("glass.refraction", 18),
            highlight = prefs.getInt("glass.highlight", 55), depth = prefs.getInt("glass.depth", 8),
            border = prefs.getInt("glass.border", 35), cornerRadius = prefs.getInt("glass.radius", 28)
        ),
        motion = MotionSettings(
            preset = enum("motion.preset", MotionPreset.FLUID), speed = prefs.getInt("motion.speed", 100),
            springStrength = prefs.getInt("motion.spring", 420), damping = prefs.getInt("motion.damping", 32),
            bounce = prefs.getInt("motion.bounce", 18), gestureSensitivity = prefs.getInt("motion.sensitivity", 70)
        ),
        island = IslandSettings(
            enabled = prefs.getBoolean("island.enabled", true), cutoutAware = prefs.getBoolean("island.cutoutAware", true),
            width = prefs.getInt("island.width", 96), height = prefs.getInt("island.height", 32), radius = prefs.getInt("island.radius", 18),
            topOffset = prefs.getInt("island.offset", 10), showNotifications = prefs.getBoolean("island.notifications", true),
            showMedia = prefs.getBoolean("island.media", true), showCalls = prefs.getBoolean("island.calls", true),
            showTimers = prefs.getBoolean("island.timers", true), showRecording = prefs.getBoolean("island.recording", true),
            showNavigation = prefs.getBoolean("island.navigation", true), blurSurroundingCutout = prefs.getBoolean("island.cutoutBlur", true)
        ),
        quickSpace = QuickSpaceSettings(
            enabled = prefs.getBoolean("quick.enabled", true), edge = enum("quick.edge", QuickEdge.RIGHT),
            handleThickness = prefs.getInt("quick.thickness", 5), handleLength = prefs.getInt("quick.length", 42),
            triggerDistance = prefs.getInt("quick.trigger", 80), sensitivity = prefs.getInt("quick.sensitivity", 70),
            panelWidth = prefs.getInt("quick.panelWidth", 88), panelHeight = prefs.getInt("quick.panelHeight", 42),
            notes = prefs.getBoolean("quick.notes", true), recorder = prefs.getBoolean("quick.recorder", true),
            calculator = prefs.getBoolean("quick.calculator", true), screenshot = prefs.getBoolean("quick.screenshot", true),
            clipboard = prefs.getBoolean("quick.clipboard", true), recentApps = prefs.getBoolean("quick.recent", true), shortcuts = prefs.getBoolean("quick.shortcuts", true)
        ),
        controlCenter = ControlCenterSettings(
            enabled = prefs.getBoolean("cc.enabled", true), edge = enum("cc.edge", ControlEdge.TOP_RIGHT),
            columns = prefs.getInt("cc.columns", 2), tileRadius = prefs.getInt("cc.tileRadius", 24), panelRadius = prefs.getInt("cc.panelRadius", 32),
            showBrightness = prefs.getBoolean("cc.brightness", true), showVolume = prefs.getBoolean("cc.volume", true),
            showWifi = prefs.getBoolean("cc.wifi", true), showBluetooth = prefs.getBoolean("cc.bluetooth", true),
            showAirplane = prefs.getBoolean("cc.airplane", true), showRotation = prefs.getBoolean("cc.rotation", true),
            showFlashlight = prefs.getBoolean("cc.flashlight", true), showDnd = prefs.getBoolean("cc.dnd", true)
        ),
        floatingWindows = FloatingWindowSettings(
            enabled = prefs.getBoolean("float.enabled", true), defaultWidth = prefs.getInt("float.width", 72), defaultHeight = prefs.getInt("float.height", 58),
            radius = prefs.getInt("float.radius", 26), transparency = prefs.getInt("float.transparency", 92), shadow = prefs.getInt("float.shadow", 70),
            maxWindows = prefs.getInt("float.max", 3), edgeResize = prefs.getBoolean("float.edgeResize", true), cornerResize = prefs.getBoolean("float.cornerResize", true),
            snapWindows = prefs.getBoolean("float.snap", true), rememberPositions = prefs.getBoolean("float.remember", true)
        ),
        splitScreen = SplitScreenSettings(
            enabled = prefs.getBoolean("split.enabled", true), defaultRatio = prefs.getInt("split.ratio", 50),
            allowCustomRatio = prefs.getBoolean("split.custom", true), rememberLayouts = prefs.getBoolean("split.remember", true),
            snapToEdges = prefs.getBoolean("split.snap", true), dividerThickness = prefs.getInt("split.divider", 4)
        ),
        security = SecuritySettings(
            appLockEnabled = prefs.getBoolean("security.enabled", true), relockOnLeave = prefs.getBoolean("security.leave", true),
            relockOnScreenOff = prefs.getBoolean("security.screenOff", true), relockTimeoutSeconds = prefs.getInt("security.timeout", 0),
            useBiometrics = prefs.getBoolean("security.biometric", true), useDeviceCredential = prefs.getBoolean("security.credential", true),
            hideLockedApps = prefs.getBoolean("security.hide", false)
        ),
        appearance = AppearanceSettings(
            theme = enum("appearance.theme", AppearanceTheme.SYSTEM), iconPack = prefs.getString("appearance.iconPack", "System") ?: "System",
            wallpaperDim = prefs.getInt("appearance.dim", 8), showTime = prefs.getBoolean("appearance.time", true), showAetherLabel = prefs.getBoolean("appearance.label", true)
        )
    )

    @Synchronized fun save(s: AetherSettings) {
        prefs.edit().apply {
            putBoolean("setupComplete", s.setupComplete); putEnum("homeMode", s.homeMode)
            putInt("grid.columns", s.grid.columns); putInt("grid.rows", s.grid.rows); putInt("grid.iconSize", s.grid.iconSize)
            putInt("grid.hSpacing", s.grid.horizontalSpacing); putInt("grid.vSpacing", s.grid.verticalSpacing); putInt("grid.hPad", s.grid.horizontalPadding); putInt("grid.vPad", s.grid.verticalPadding)
            putBoolean("grid.labels", s.grid.showLabels); putInt("grid.labelSize", s.grid.labelSize); putEnum("grid.folderStyle", s.grid.folderStyle); putInt("grid.folderRadius", s.grid.folderRadius)
            putBoolean("dock.enabled", s.dock.enabled); putInt("dock.count", s.dock.appCount); putInt("dock.radius", s.dock.radius); putInt("dock.height", s.dock.height); putInt("dock.bottom", s.dock.bottomPadding)
            putBoolean("dock.autoGroup", s.dock.autoGroup); putBoolean("dock.company", s.dock.groupByCompany); putBoolean("dock.category", s.dock.groupByCategory); putBoolean("dock.smartGroups", s.dock.smartGroups)
            putInt("glass.opacity", s.glass.opacity); putInt("glass.blur", s.glass.blur); putInt("glass.saturation", s.glass.saturation); putInt("glass.refraction", s.glass.refraction); putInt("glass.highlight", s.glass.highlight); putInt("glass.depth", s.glass.depth); putInt("glass.border", s.glass.border); putInt("glass.radius", s.glass.cornerRadius)
            putEnum("motion.preset", s.motion.preset); putInt("motion.speed", s.motion.speed); putInt("motion.spring", s.motion.springStrength); putInt("motion.damping", s.motion.damping); putInt("motion.bounce", s.motion.bounce); putInt("motion.sensitivity", s.motion.gestureSensitivity)
            putBoolean("island.enabled", s.island.enabled); putBoolean("island.cutoutAware", s.island.cutoutAware); putInt("island.width", s.island.width); putInt("island.height", s.island.height); putInt("island.radius", s.island.radius); putInt("island.offset", s.island.topOffset)
            putBoolean("island.notifications", s.island.showNotifications); putBoolean("island.media", s.island.showMedia); putBoolean("island.calls", s.island.showCalls); putBoolean("island.timers", s.island.showTimers); putBoolean("island.recording", s.island.showRecording); putBoolean("island.navigation", s.island.showNavigation); putBoolean("island.cutoutBlur", s.island.blurSurroundingCutout)
            putBoolean("quick.enabled", s.quickSpace.enabled); putEnum("quick.edge", s.quickSpace.edge); putInt("quick.thickness", s.quickSpace.handleThickness); putInt("quick.length", s.quickSpace.handleLength); putInt("quick.trigger", s.quickSpace.triggerDistance); putInt("quick.sensitivity", s.quickSpace.sensitivity); putInt("quick.panelWidth", s.quickSpace.panelWidth); putInt("quick.panelHeight", s.quickSpace.panelHeight)
            putBoolean("quick.notes", s.quickSpace.notes); putBoolean("quick.recorder", s.quickSpace.recorder); putBoolean("quick.calculator", s.quickSpace.calculator); putBoolean("quick.screenshot", s.quickSpace.screenshot); putBoolean("quick.clipboard", s.quickSpace.clipboard); putBoolean("quick.recent", s.quickSpace.recentApps); putBoolean("quick.shortcuts", s.quickSpace.shortcuts)
            putBoolean("cc.enabled", s.controlCenter.enabled); putEnum("cc.edge", s.controlCenter.edge); putInt("cc.columns", s.controlCenter.columns); putInt("cc.tileRadius", s.controlCenter.tileRadius); putInt("cc.panelRadius", s.controlCenter.panelRadius)
            putBoolean("cc.brightness", s.controlCenter.showBrightness); putBoolean("cc.volume", s.controlCenter.showVolume); putBoolean("cc.wifi", s.controlCenter.showWifi); putBoolean("cc.bluetooth", s.controlCenter.showBluetooth); putBoolean("cc.airplane", s.controlCenter.showAirplane); putBoolean("cc.rotation", s.controlCenter.showRotation); putBoolean("cc.flashlight", s.controlCenter.showFlashlight); putBoolean("cc.dnd", s.controlCenter.showDnd)
            putBoolean("float.enabled", s.floatingWindows.enabled); putInt("float.width", s.floatingWindows.defaultWidth); putInt("float.height", s.floatingWindows.defaultHeight); putInt("float.radius", s.floatingWindows.radius); putInt("float.transparency", s.floatingWindows.transparency); putInt("float.shadow", s.floatingWindows.shadow); putInt("float.max", s.floatingWindows.maxWindows); putBoolean("float.edgeResize", s.floatingWindows.edgeResize); putBoolean("float.cornerResize", s.floatingWindows.cornerResize); putBoolean("float.snap", s.floatingWindows.snapWindows); putBoolean("float.remember", s.floatingWindows.rememberPositions)
            putBoolean("split.enabled", s.splitScreen.enabled); putInt("split.ratio", s.splitScreen.defaultRatio); putBoolean("split.custom", s.splitScreen.allowCustomRatio); putBoolean("split.remember", s.splitScreen.rememberLayouts); putBoolean("split.snap", s.splitScreen.snapToEdges); putInt("split.divider", s.splitScreen.dividerThickness)
            putBoolean("security.enabled", s.security.appLockEnabled); putBoolean("security.leave", s.security.relockOnLeave); putBoolean("security.screenOff", s.security.relockOnScreenOff); putInt("security.timeout", s.security.relockTimeoutSeconds); putBoolean("security.biometric", s.security.useBiometrics); putBoolean("security.credential", s.security.useDeviceCredential); putBoolean("security.hide", s.security.hideLockedApps)
            putEnum("appearance.theme", s.appearance.theme); putString("appearance.iconPack", s.appearance.iconPack); putInt("appearance.dim", s.appearance.wallpaperDim); putBoolean("appearance.time", s.appearance.showTime); putBoolean("appearance.label", s.appearance.showAetherLabel)
            apply()
        }
    }

    fun markSetupComplete() = save(load().copy(setupComplete = true))
    fun resetSetup() = save(AetherSettings())

    private inline fun <reified E : Enum<E>> enum(key: String, fallback: E): E = runCatching { enumValueOf<E>(prefs.getString(key, fallback.name) ?: fallback.name) }.getOrDefault(fallback)
    private fun <E : Enum<E>> android.content.SharedPreferences.Editor.putEnum(key: String, value: E) = putString(key, value.name)
}
