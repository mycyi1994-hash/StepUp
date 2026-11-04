#!/usr/bin/env python3
"""임시 진단 — 에뮬레이터를 죽이는 그리기를 가르려고 화면 코드를 한 군데씩 바꾼다(.github/workflows/emulator-crash-diag.yml)."""
import sys
UI = "sharedUi/src/commonMain/kotlin/com/stepup/android/ui/"
DETAIL = UI + "screens/community/crew/CrewDetailScreen.kt"
CARD = UI + "components/StepUpUi.kt"
NAV = UI + "MainScaffold.kt"
SHOE = UI + "components/ShoeCard.kt"
VARIANTS = {
    "base": [],
    "crewNoPoster": [(DETAIL, "            CrewPoster(card, onBack = actions.onBack, onImage = actions.onOpenImage, onMore = onMore,\n                onShare = { shareCrew(card) })",
                      "            Spacer(Modifier.height(330.dp).testTag(\"crew-detail-image\"))")],
    "crewNoPhoto": [(DETAIL, "        custom ?: place?.painter, colorFilter", "        null, colorFilter")],
    "crewNoGradient": [(CARD, "        Box(\n            Modifier.matchParentSize().background(\n                Brush.verticalGradient(\n                    0f to Color.Black.copy(alpha = .28f)",
                        "        if (false) Box(\n            Modifier.matchParentSize().background(\n                Brush.verticalGradient(\n                    0f to Color.Black.copy(alpha = .28f)")],
    "crewNoTransition": [(NAV, "            enterTransition = { fadeIn(tween(motion.duration(180))) + slideInHorizontally(tween(motion.duration(220))) { if (motion.reduced) 0 else it / 18 } },\n            exitTransition = { fadeOut(tween(motion.duration(140))) },\n            popEnterTransition = { fadeIn(tween(motion.duration(180))) },\n            popExitTransition = { fadeOut(tween(motion.duration(140))) + slideOutHorizontally(tween(motion.duration(220))) { if (motion.reduced) 0 else it / 18 } },",
                          "            enterTransition = { androidx.compose.animation.EnterTransition.None },\n            exitTransition = { androidx.compose.animation.ExitTransition.None },\n            popEnterTransition = { androidx.compose.animation.EnterTransition.None },\n            popExitTransition = { androidx.compose.animation.ExitTransition.None },")],
    "shoesBase": [],
    "shoesNoBlur": [(SHOE, "modifier = Modifier.matchParentSize().blur(5.dp))", "modifier = Modifier.matchParentSize())")],
    "shoesNoTilt": [(SHOE, "                rotationX = t.rx.value; rotationY = t.ry.value; scaleX = t.s.value; scaleY = t.s.value\n                cameraDistance = 12f * density.density",
                     "                scaleX = t.s.value; scaleY = t.s.value")],
}
G_OLD = '        Box(\n            Modifier.matchParentSize().background(\n                Brush.verticalGradient(\n                    0f to Color.Black.copy(alpha = .28f), .3f to Color.Transparent, .5f to Color.Black.copy(alpha = .05f),\n                    1f to Color.Black.copy(alpha = .78f),\n                ),\n            ),\n        )'
def grad(body):
    return [(CARD, G_OLD, body)]
VARIANTS.update({
    "fixFinite": grad("        Box(Modifier.matchParentSize().drawWithCache {\n"
        "            val b = Brush.verticalGradient(0f to Color.Black.copy(alpha = .28f), .3f to Color.Transparent, .5f to Color.Black.copy(alpha = .05f),\n"
        "                1f to Color.Black.copy(alpha = .78f), startY = 0f, endY = size.height)\n"
        "            onDrawBehind { drawRect(b) }\n        })") + [(CARD, "import androidx.compose.ui.graphics.graphicsLayer\n", "import androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.draw.drawWithCache\n")],
    "fixEven": grad("        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .28f), Color.Transparent, Color.Black.copy(alpha = .05f), Color.Black.copy(alpha = .78f)))))"),
    "fixTwo": grad("        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .05f), Color.Black.copy(alpha = .78f)))))"),
    "fixNoTransparent": grad("        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .28f), .3f to Color.Black.copy(alpha = .001f), .5f to Color.Black.copy(alpha = .05f), 1f to Color.Black.copy(alpha = .78f))))"),
    "fixLayer": grad(G_OLD.replace("Modifier.matchParentSize().background(", "Modifier.matchParentSize().graphicsLayer().background(")),
    "shoesNoGradient": [(SHOE, "            Box(\n                Modifier.matchParentSize().background(\n                    if (dark) Brush.verticalGradient", "            if (false) Box(\n                Modifier.matchParentSize().background(\n                    if (dark) Brush.verticalGradient")],
})
name = sys.argv[1]
for path, old, new in VARIANTS[name]:
    s = open(path, encoding="utf-8").read()
    assert s.count(old) == 1, (name, path, s.count(old))
    open(path, "w", encoding="utf-8").write(s.replace(old, new))
print("variant", name, "applied", len(VARIANTS[name]), "edits")
