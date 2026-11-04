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
name = sys.argv[1]
for path, old, new in VARIANTS[name]:
    s = open(path, encoding="utf-8").read()
    assert s.count(old) == 1, (name, path, s.count(old))
    open(path, "w", encoding="utf-8").write(s.replace(old, new))
print("variant", name, "applied", len(VARIANTS[name]), "edits")
