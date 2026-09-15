package com.aether.launcher.settings

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.roundToInt

/** First-run wizard. Every step writes through the same AetherSettings model used by Settings. */
class AetherSetupView(context: Context, private val onFinished: () -> Unit) : ScrollView(context) {
    private val store = AetherSettingsStore(context)
    private var settings = store.load()
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(38), dp(24), dp(28)) }
    private var page = 0
    private val pages = listOf("Welcome", "Home", "Grid", "Appearance", "Dock", "Dynamic Island", "Quick Space", "Control Center", "Windows", "Security", "Ready")

    init {
        setFillViewport(true)
        setBackgroundColor(Color.rgb(8, 10, 14))
        addView(content)
        render()
    }

    private fun render() {
        content.removeAllViews()
        val step = TextView(context).apply { text = "AETHER  •  ${page + 1}/${pages.size}"; setTextColor(0x99FFFFFF.toInt()); textSize = 11f; letterSpacing = .16f }
        content.addView(step, lp(-1, 30))
        val title = TextView(context).apply { text = pages[page]; setTextColor(Color.WHITE); textSize = if (page == 0 || page == pages.lastIndex) 34f else 27f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12), 0, dp(8)) }
        content.addView(title, lp(-1, -2))
        val body = TextView(context).apply { text = pageSubtitle(page); setTextColor(0xB8FFFFFF.toInt()); textSize = 15f; setPadding(0, 0, 0, dp(22)) }
        content.addView(body, lp(-1, -2))

        when (page) {
            0 -> welcome()
            1 -> home()
            2 -> grid()
            3 -> appearance()
            4 -> dock()
            5 -> island()
            6 -> quickSpace()
            7 -> controlCenter()
            8 -> windows()
            9 -> security()
            10 -> ready()
        }

        val nav = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(22), 0, 0) }
        if (page > 0) nav.addView(button("Back", false) { page--; render() }, LinearLayout.LayoutParams(0, dp(56), 1f))
        val nextText = if (page == pages.lastIndex) "Enter Aether" else "Continue"
        nav.addView(button(nextText, true) { next() }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { if (page > 0) marginStart = dp(12) })
        content.addView(nav)
    }

    private fun next() {
        if (page < pages.lastIndex) { page++; render() }
        else { store.save(settings.copy(setupComplete = true)); onFinished() }
    }

    private fun welcome() {
        val card = card().apply { setPadding(dp(26), dp(30), dp(26), dp(30)) }
        card.addView(text("Hello.", 48f, Color.WHITE, true))
        card.addView(text("A new kind of Android home.", 19f, 0xCCFFFFFF.toInt(), false), lp(-1, -2))
        card.addView(spacer(24))
        card.addView(text("Aether combines a calm home screen with Quick Space, Dynamic Island, liquid-glass surfaces, floating windows and a powerful control layer. You can change every choice later in Aether Settings.", 14f, 0xB5FFFFFF.toInt(), false), lp(-1, -2))
        content.addView(card)
    }

    private fun home() {
        val group = radioGroup()
        HomeMode.values().forEach { mode ->
            val label = when (mode) { HomeMode.ALL_APPS -> "All apps on Home"; HomeMode.HOME_AND_DRAWER -> "Home + App Drawer"; HomeMode.DRAWER_ONLY -> "App Drawer + minimal Home" }
            addRadio(group, label, settings.homeMode == mode) { settings = settings.copy(homeMode = mode); store.save(settings) }
        }
        content.addView(group)
        content.addView(preview("Your home updates as you choose", 150))
    }

    private fun grid() {
        content.addView(slider("Apps per row", 3, 7, settings.grid.columns) { settings = settings.copy(grid = settings.grid.copy(columns = it)); store.save(settings) })
        content.addView(slider("Rows", 4, 9, settings.grid.rows) { settings = settings.copy(grid = settings.grid.copy(rows = it)); store.save(settings) })
        content.addView(slider("Icon size", 40, 72, settings.grid.iconSize) { settings = settings.copy(grid = settings.grid.copy(iconSize = it)); store.save(settings) })
        content.addView(slider("Horizontal spacing", 4, 28, settings.grid.horizontalSpacing) { settings = settings.copy(grid = settings.grid.copy(horizontalSpacing = it)); store.save(settings) })
        content.addView(slider("Vertical spacing", 6, 32, settings.grid.verticalSpacing) { settings = settings.copy(grid = settings.grid.copy(verticalSpacing = it)); store.save(settings) })
        content.addView(slider("Horizontal padding", 8, 40, settings.grid.horizontalPadding) { settings = settings.copy(grid = settings.grid.copy(horizontalPadding = it)); store.save(settings) })
        content.addView(slider("Vertical padding", 8, 48, settings.grid.verticalPadding) { settings = settings.copy(grid = settings.grid.copy(verticalPadding = it)); store.save(settings) })
        content.addView(toggle("Show app labels", settings.grid.showLabels) { settings = settings.copy(grid = settings.grid.copy(showLabels = it)); store.save(settings) })
        content.addView(slider("Label size", 9, 16, settings.grid.labelSize) { settings = settings.copy(grid = settings.grid.copy(labelSize = it)); store.save(settings) })
        content.addView(choice("Folder style", FolderStyle.values().toList(), settings.grid.folderStyle) { settings = settings.copy(grid = settings.grid.copy(folderStyle = it)); store.save(settings) })
        content.addView(slider("Folder corner radius", 8, 40, settings.grid.folderRadius) { settings = settings.copy(grid = settings.grid.copy(folderRadius = it)); store.save(settings) })
        content.addView(preview("Live grid preview", 180))
    }

    private fun appearance() {
        content.addView(choice("Theme", AppearanceTheme.values().toList(), settings.appearance.theme) { settings = settings.copy(appearance = settings.appearance.copy(theme = it)); store.save(settings) })
        content.addView(slider("Corner radius", 8, 42, settings.glass.cornerRadius) { settings = settings.copy(glass = settings.glass.copy(cornerRadius = it)); store.save(settings) })
        content.addView(slider("Glass opacity", 45, 100, settings.glass.opacity) { settings = settings.copy(glass = settings.glass.copy(opacity = it)); store.save(settings) })
        content.addView(slider("Blur", 0, 50, settings.glass.blur) { settings = settings.copy(glass = settings.glass.copy(blur = it)); store.save(settings) })
        content.addView(slider("Saturation", 80, 135, settings.glass.saturation) { settings = settings.copy(glass = settings.glass.copy(saturation = it)); store.save(settings) })
        content.addView(slider("Refraction", 0, 40, settings.glass.refraction) { settings = settings.copy(glass = settings.glass.copy(refraction = it)); store.save(settings) })
        content.addView(slider("Highlights", 0, 100, settings.glass.highlight) { settings = settings.copy(glass = settings.glass.copy(highlight = it)); store.save(settings) })
        content.addView(slider("Depth", 0, 20, settings.glass.depth) { settings = settings.copy(glass = settings.glass.copy(depth = it)); store.save(settings) })
        content.addView(slider("Border", 0, 100, settings.glass.border) { settings = settings.copy(glass = settings.glass.copy(border = it)); store.save(settings) })
        content.addView(choice("Motion preset", MotionPreset.values().toList(), settings.motion.preset) { settings = settings.copy(motion = settings.motion.copy(preset = it)); store.save(settings) })
        content.addView(slider("Motion speed", 50, 160, settings.motion.speed) { settings = settings.copy(motion = settings.motion.copy(speed = it)); store.save(settings) })
        content.addView(slider("Spring strength", 150, 800, settings.motion.springStrength) { settings = settings.copy(motion = settings.motion.copy(springStrength = it)); store.save(settings) })
        content.addView(slider("Damping", 10, 80, settings.motion.damping) { settings = settings.copy(motion = settings.motion.copy(damping = it)); store.save(settings) })
        content.addView(slider("Bounce", 0, 40, settings.motion.bounce) { settings = settings.copy(motion = settings.motion.copy(bounce = it)); store.save(settings) })
        content.addView(slider("Gesture sensitivity", 10, 100, settings.motion.gestureSensitivity) { settings = settings.copy(motion = settings.motion.copy(gestureSensitivity = it)); store.save(settings) })
        content.addView(preview("Glass + motion preview", 180))
    }

    private fun dock() {
        content.addView(toggle("Use bottom dock", settings.dock.enabled) { settings = settings.copy(dock = settings.dock.copy(enabled = it)); store.save(settings) })
        content.addView(slider("Dock apps", 3, 8, settings.dock.appCount) { settings = settings.copy(dock = settings.dock.copy(appCount = it)); store.save(settings) })
        content.addView(slider("Dock height", 56, 110, settings.dock.height) { settings = settings.copy(dock = settings.dock.copy(height = it)); store.save(settings) })
        content.addView(slider("Dock corner radius", 12, 44, settings.dock.radius) { settings = settings.copy(dock = settings.dock.copy(radius = it)); store.save(settings) })
        content.addView(slider("Bottom padding", 4, 40, settings.dock.bottomPadding) { settings = settings.copy(dock = settings.dock.copy(bottomPadding = it)); store.save(settings) })
        content.addView(toggle("Auto-group similar apps", settings.dock.autoGroup) { settings = settings.copy(dock = settings.dock.copy(autoGroup = it)); store.save(settings) })
        content.addView(toggle("Group by company", settings.dock.groupByCompany) { settings = settings.copy(dock = settings.dock.copy(groupByCompany = it)); store.save(settings) })
        content.addView(toggle("Group by category", settings.dock.groupByCategory) { settings = settings.copy(dock = settings.dock.copy(groupByCategory = it)); store.save(settings) })
        content.addView(toggle("Smart groups", settings.dock.smartGroups) { settings = settings.copy(dock = settings.dock.copy(smartGroups = it)); store.save(settings) })
        content.addView(preview("Dock preview", 150))
    }

    private fun island() {
        content.addView(toggle("Dynamic Island", settings.island.enabled) { settings = settings.copy(island = settings.island.copy(enabled = it)); store.save(settings) })
        content.addView(toggle("Adapt around camera cutout", settings.island.cutoutAware) { settings = settings.copy(island = settings.island.copy(cutoutAware = it)); store.save(settings) })
        content.addView(slider("Width", 72, 180, settings.island.width) { settings = settings.copy(island = settings.island.copy(width = it)); store.save(settings) })
        content.addView(slider("Height", 24, 70, settings.island.height) { settings = settings.copy(island = settings.island.copy(height = it)); store.save(settings) })
        content.addView(slider("Corner radius", 8, 35, settings.island.radius) { settings = settings.copy(island = settings.island.copy(radius = it)); store.save(settings) })
        content.addView(slider("Top offset", 0, 40, settings.island.topOffset) { settings = settings.copy(island = settings.island.copy(topOffset = it)); store.save(settings) })
        content.addView(toggle("Blur around camera cutout", settings.island.blurSurroundingCutout) { settings = settings.copy(island = settings.island.copy(blurSurroundingCutout = it)); store.save(settings) })
        content.addView(toggle("Notifications", settings.island.showNotifications) { settings = settings.copy(island = settings.island.copy(showNotifications = it)); store.save(settings) })
        content.addView(toggle("Media", settings.island.showMedia) { settings = settings.copy(island = settings.island.copy(showMedia = it)); store.save(settings) })
        content.addView(toggle("Calls", settings.island.showCalls) { settings = settings.copy(island = settings.island.copy(showCalls = it)); store.save(settings) })
        content.addView(toggle("Timers / alarms", settings.island.showTimers) { settings = settings.copy(island = settings.island.copy(showTimers = it)); store.save(settings) })
        content.addView(toggle("Recording", settings.island.showRecording) { settings = settings.copy(island = settings.island.copy(showRecording = it)); store.save(settings) })
        content.addView(toggle("Navigation", settings.island.showNavigation) { settings = settings.copy(island = settings.island.copy(showNavigation = it)); store.save(settings) })
        content.addView(preview("Island morph preview", 140))
    }

    private fun quickSpace() {
        content.addView(toggle("Quick Space", settings.quickSpace.enabled) { settings = settings.copy(quickSpace = settings.quickSpace.copy(enabled = it)); store.save(settings) })
        content.addView(choice("Edge", QuickEdge.values().toList(), settings.quickSpace.edge) { settings = settings.copy(quickSpace = settings.quickSpace.copy(edge = it)); store.save(settings) })
        content.addView(slider("Handle thickness", 3, 12, settings.quickSpace.handleThickness) { settings = settings.copy(quickSpace = settings.quickSpace.copy(handleThickness = it)); store.save(settings) })
        content.addView(slider("Handle length", 24, 90, settings.quickSpace.handleLength) { settings = settings.copy(quickSpace = settings.quickSpace.copy(handleLength = it)); store.save(settings) })
        content.addView(slider("Trigger distance", 40, 180, settings.quickSpace.triggerDistance) { settings = settings.copy(quickSpace = settings.quickSpace.copy(triggerDistance = it)); store.save(settings) })
        content.addView(slider("Gesture sensitivity", 10, 100, settings.quickSpace.sensitivity) { settings = settings.copy(quickSpace = settings.quickSpace.copy(sensitivity = it)); store.save(settings) })
        content.addView(slider("Panel width", 70, 96, settings.quickSpace.panelWidth) { settings = settings.copy(quickSpace = settings.quickSpace.copy(panelWidth = it)); store.save(settings) })
        content.addView(slider("Panel height", 28, 70, settings.quickSpace.panelHeight) { settings = settings.copy(quickSpace = settings.quickSpace.copy(panelHeight = it)); store.save(settings) })
        listOf("Notes" to settings.quickSpace.notes, "Voice recorder" to settings.quickSpace.recorder, "Calculator" to settings.quickSpace.calculator, "Screenshot" to settings.quickSpace.screenshot, "Clipboard" to settings.quickSpace.clipboard, "Recent apps" to settings.quickSpace.recentApps, "Shortcuts" to settings.quickSpace.shortcuts).forEach { (name, value) ->
            content.addView(toggle(name, value) { settings = settings.copy(quickSpace = settings.quickSpace.copy(notes = if (name == "Notes") it else settings.quickSpace.notes, recorder = if (name == "Voice recorder") it else settings.quickSpace.recorder, calculator = if (name == "Calculator") it else settings.quickSpace.calculator, screenshot = if (name == "Screenshot") it else settings.quickSpace.screenshot, clipboard = if (name == "Clipboard") it else settings.quickSpace.clipboard, recentApps = if (name == "Recent apps") it else settings.quickSpace.recentApps, shortcuts = if (name == "Shortcuts") it else settings.quickSpace.shortcuts)); store.save(settings) })
        }
        content.addView(preview("Edge handle preview", 150))
    }

    private fun controlCenter() {
        content.addView(toggle("Control Center", settings.controlCenter.enabled) { settings = settings.copy(controlCenter = settings.controlCenter.copy(enabled = it)); store.save(settings) })
        content.addView(choice("Gesture corner", ControlEdge.values().toList(), settings.controlCenter.edge) { settings = settings.copy(controlCenter = settings.controlCenter.copy(edge = it)); store.save(settings) })
        content.addView(slider("Tile columns", 1, 4, settings.controlCenter.columns) { settings = settings.copy(controlCenter = settings.controlCenter.copy(columns = it)); store.save(settings) })
        content.addView(slider("Tile radius", 10, 40, settings.controlCenter.tileRadius) { settings = settings.copy(controlCenter = settings.controlCenter.copy(tileRadius = it)); store.save(settings) })
        content.addView(slider("Panel radius", 12, 44, settings.controlCenter.panelRadius) { settings = settings.copy(controlCenter = settings.controlCenter.copy(panelRadius = it)); store.save(settings) })
        listOf("Brightness" to settings.controlCenter.showBrightness, "Volume" to settings.controlCenter.showVolume, "Wi-Fi" to settings.controlCenter.showWifi, "Bluetooth" to settings.controlCenter.showBluetooth, "Airplane mode" to settings.controlCenter.showAirplane, "Rotation" to settings.controlCenter.showRotation, "Flashlight" to settings.controlCenter.showFlashlight, "Do Not Disturb" to settings.controlCenter.showDnd).forEach { (name, value) ->
            content.addView(toggle(name, value) { settings = settings.copy(controlCenter = settings.controlCenter.copy(showBrightness = if (name == "Brightness") it else settings.controlCenter.showBrightness, showVolume = if (name == "Volume") it else settings.controlCenter.showVolume, showWifi = if (name == "Wi-Fi") it else settings.controlCenter.showWifi, showBluetooth = if (name == "Bluetooth") it else settings.controlCenter.showBluetooth, showAirplane = if (name == "Airplane mode") it else settings.controlCenter.showAirplane, showRotation = if (name == "Rotation") it else settings.controlCenter.showRotation, showFlashlight = if (name == "Flashlight") it else settings.controlCenter.showFlashlight, showDnd = if (name == "Do Not Disturb") it else settings.controlCenter.showDnd)); store.save(settings) })
        }
        content.addView(preview("Control Center preview", 190))
    }

    private fun windows() {
        content.addView(toggle("Floating windows", settings.floatingWindows.enabled) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(enabled = it)); store.save(settings) })
        content.addView(slider("Default width", 50, 95, settings.floatingWindows.defaultWidth) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(defaultWidth = it)); store.save(settings) })
        content.addView(slider("Default height", 40, 90, settings.floatingWindows.defaultHeight) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(defaultHeight = it)); store.save(settings) })
        content.addView(slider("Window radius", 10, 42, settings.floatingWindows.radius) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(radius = it)); store.save(settings) })
        content.addView(slider("Window opacity", 55, 100, settings.floatingWindows.transparency) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(transparency = it)); store.save(settings) })
        content.addView(slider("Shadow", 0, 100, settings.floatingWindows.shadow) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(shadow = it)); store.save(settings) })
        content.addView(slider("Maximum windows", 1, 6, settings.floatingWindows.maxWindows) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(maxWindows = it)); store.save(settings) })
        content.addView(toggle("Resize from edges", settings.floatingWindows.edgeResize) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(edgeResize = it)); store.save(settings) })
        content.addView(toggle("Resize from corners", settings.floatingWindows.cornerResize) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(cornerResize = it)); store.save(settings) })
        content.addView(toggle("Snap windows", settings.floatingWindows.snapWindows) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(snapWindows = it)); store.save(settings) })
        content.addView(toggle("Remember positions", settings.floatingWindows.rememberPositions) { settings = settings.copy(floatingWindows = settings.floatingWindows.copy(rememberPositions = it)); store.save(settings) })
        content.addView(dividerLabel("Split screen"))
        content.addView(toggle("Split screen", settings.splitScreen.enabled) { settings = settings.copy(splitScreen = settings.splitScreen.copy(enabled = it)); store.save(settings) })
        content.addView(slider("Default split ratio", 30, 70, settings.splitScreen.defaultRatio) { settings = settings.copy(splitScreen = settings.splitScreen.copy(defaultRatio = it)); store.save(settings) })
        content.addView(toggle("Allow custom ratio", settings.splitScreen.allowCustomRatio) { settings = settings.copy(splitScreen = settings.splitScreen.copy(allowCustomRatio = it)); store.save(settings) })
        content.addView(toggle("Remember split layouts", settings.splitScreen.rememberLayouts) { settings = settings.copy(splitScreen = settings.splitScreen.copy(rememberLayouts = it)); store.save(settings) })
        content.addView(toggle("Snap divider", settings.splitScreen.snapToEdges) { settings = settings.copy(splitScreen = settings.splitScreen.copy(snapToEdges = it)); store.save(settings) })
        content.addView(slider("Divider thickness", 2, 12, settings.splitScreen.dividerThickness) { settings = settings.copy(splitScreen = settings.splitScreen.copy(dividerThickness = it)); store.save(settings) })
        content.addView(preview("Window + split preview", 180))
    }

    private fun security() {
        content.addView(toggle("App Lock", settings.security.appLockEnabled) { settings = settings.copy(security = settings.security.copy(appLockEnabled = it)); store.save(settings) })
        content.addView(toggle("Relock when leaving app", settings.security.relockOnLeave) { settings = settings.copy(security = settings.security.copy(relockOnLeave = it)); store.save(settings) })
        content.addView(toggle("Relock when screen turns off", settings.security.relockOnScreenOff) { settings = settings.copy(security = settings.security.copy(relockOnScreenOff = it)); store.save(settings) })
        content.addView(slider("Relock timeout (0 = immediately)", 0, 300, settings.security.relockTimeoutSeconds) { settings = settings.copy(security = settings.security.copy(relockTimeoutSeconds = it)); store.save(settings) })
        content.addView(toggle("Use biometrics", settings.security.useBiometrics) { settings = settings.copy(security = settings.security.copy(useBiometrics = it)); store.save(settings) })
        content.addView(toggle("Use device credential", settings.security.useDeviceCredential) { settings = settings.copy(security = settings.security.copy(useDeviceCredential = it)); store.save(settings) })
        content.addView(toggle("Hide locked apps", settings.security.hideLockedApps) { settings = settings.copy(security = settings.security.copy(hideLockedApps = it)); store.save(settings) })
        content.addView(actionCard("Permissions & system access", "Configure launcher, overlay, accessibility, notifications and Shizuku access later in Aether Settings.") {})
    }

    private fun ready() {
        content.addView(preview("Aether is configured", 220))
        content.addView(text("Everything you picked here remains editable. Aether Settings contains the same controls after setup.", 15f, 0xC8FFFFFF.toInt(), false), lp(-1, -2))
        content.addView(spacer(18))
        content.addView(actionCard("Setup Center", "You can revisit permissions and advanced system integrations at any time.") {})
    }

    private fun pageSubtitle(p: Int): String = when (p) {
        0 -> "Let's build your launcher around the way you actually use your phone."
        1 -> "Choose how apps live on your Home screen."
        2 -> "Tune density, spacing, icons, labels and folders."
        3 -> "Shape the glass, corners, lighting and motion."
        4 -> "Build your bottom bar and smart app groups."
        5 -> "Configure the living activity surface at the top of your display."
        6 -> "Make Quick Space your fast lane for actions and tools."
        7 -> "Design the swipe-down control surface around your habits."
        8 -> "Configure floating windows and split-screen behavior."
        9 -> "Protect selected apps and define when they lock again."
        else -> "Your Aether setup is ready."
    }

    private fun preview(label: String, height: Int): View = object : View(context) {
        private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: android.graphics.Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); p.color = 0x181F2933; c.drawRoundRect(0f, 0f, w, h, 28f, 28f, p)
            p.color = 0x20FFFFFF; c.drawRoundRect(12f, 12f, w-12f, h-12f, 22f, 22f, p)
            p.color = Color.WHITE; p.textAlign = android.graphics.Paint.Align.CENTER; p.textSize = 13f; c.drawText(label, w/2, 30f, p)
            p.color = 0xB0FFFFFF.toInt(); c.drawCircle(w/2, h*.55f, minOf(w*.12f, 38f), p)
            p.color = 0x30FFFFFF; c.drawRoundRect(w*.15f, h*.72f, w*.85f, h*.82f, 22f, 22f, p)
        }
    }.also { it.layoutParams = lp(-1, dp(height)) }

    private fun card(): LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0x161B222A); setPadding(dp(18), dp(18), dp(18), dp(18)); clipToOutline = true; outlineProvider = android.view.ViewOutlineProvider.BACKGROUND }
    private fun radioGroup() = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL; setBackgroundColor(0x141B222A) }
    private fun addRadio(group: RadioGroup, label: String, checked: Boolean, action: () -> Unit) { val r = RadioButton(context).apply { text = label; setTextColor(Color.WHITE); textSize = 15f; isChecked = checked; setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { action() } }; group.addView(r, RadioGroup.LayoutParams(-1, dp(56))) }
    private fun slider(label: String, min: Int, max: Int, value: Int, action: (Int) -> Unit): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(5)) }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val t = TextView(context).apply { text = label; setTextColor(Color.WHITE); textSize = 14f }
        val v = TextView(context).apply { text = value.toString(); setTextColor(0xAFFFFFFF.toInt()); textSize = 12f; gravity = Gravity.END }
        row.addView(t, LinearLayout.LayoutParams(0, dp(30), 1f)); row.addView(v, LinearLayout.LayoutParams(dp(52), dp(30)))
        val s = SeekBar(context).apply { max = max-min; progress = (value-min).coerceIn(0, max-min); setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { val n=p+min; v.text=n.toString(); if(fromUser) action(n) }; override fun onStartTrackingTouch(sb: SeekBar?){}; override fun onStopTrackingTouch(sb: SeekBar?){} }) }
        box.addView(row); box.addView(s, LinearLayout.LayoutParams(-1, dp(42))); return box
    }
    private fun toggle(label: String, value: Boolean, action: (Boolean) -> Unit): View { val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4), 0, dp(4)) }; row.addView(text(label, 14f, Color.WHITE, false), LinearLayout.LayoutParams(0, dp(54), 1f)); row.addView(Switch(context).apply { isChecked=value; setOnCheckedChangeListener { _, checked -> action(checked) } }, LinearLayout.LayoutParams(dp(62), dp(50))); return row }
    private fun <E : Enum<E>> choice(label: String, values: List<E>, selected: E, action: (E) -> Unit): View { val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4),0,dp(4)) }; row.addView(text(label,14f,Color.WHITE,false), LinearLayout.LayoutParams(0,dp(54),1f)); val sp=Spinner(context); sp.adapter=ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values.map { it.name.replace('_',' ').lowercase().replaceFirstChar { ch -> ch.uppercase() } }); sp.setSelection(values.indexOf(selected)); sp.onItemSelectedListener=object: android.widget.AdapterView.OnItemSelectedListener{override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){action(values[pos])};override fun onNothingSelected(p:android.widget.AdapterView<*>?) {}}; row.addView(sp,LinearLayout.LayoutParams(dp(170),dp(54)));return row }
    private fun actionCard(title: String, body: String, action: () -> Unit): View = Button(context).apply { text = "$title\n$body"; setTextColor(Color.WHITE); textSize=13f; gravity=Gravity.START or Gravity.CENTER_VERTICAL; setPadding(dp(18),0,dp(18),0); setOnClickListener { action() } }
    private fun dividerLabel(title: String): View = text(title, 19f, Color.WHITE, true).also { it.setPadding(0,dp(22),0,dp(10)) }
    private fun text(s: String, size: Float, color: Int, bold: Boolean): TextView = TextView(context).apply { text=s; textSize=size; setTextColor(color); if(bold) typeface=Typeface.DEFAULT_BOLD }
    private fun button(label:String, primary:Boolean, action:()->Unit):Button=Button(context).apply{text=label;setTextColor(Color.WHITE);textSize=14f;setOnClickListener{action()};alpha=if(primary)1f else .78f}
    private fun spacer(dp:Int):View=Space(context).also{it.layoutParams=lp(1,dp)}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(if(w==-1) -1 else dp(w),if(h==-1)-1 else dp(h))
    private fun dp(v:Int)= (v*resources.displayMetrics.density).roundToInt()
}
