package com.stepup.android.domain

// tools/gen_shoe_catalog.py 가 design/shoes-2026-09/catalog.json 으로 만든 파일 — 손으로 고치지 않는다.

/**
 * 새 신발 도감(2026-09-28) — 뽑기에서 나오는 신발. 번호는 서버(sneaker_models) · 체인(v3 모델 번호)과 같다.
 * 등급은 그림 폴더 그대로다: grade-1 레어, grade-2 에픽, grade-3 레전더리(레전더리 · 레드라인 · 피니시 시리즈).
 * 예전 52종(속성 × 변형)은 [SneakerDesigns] 에 그대로 있다 — 이미 가진 신발과 첫 신발이 쓴다.
 */
object ShoeCatalog {
    data class Model(val id: Int, val rarity: Rarity, val series: String, val englishName: String)

    val models: List<Model> = listOf(
        Model(1101, Rarity.RARE, "RARE", "Twin Arch Slide"),
        Model(1102, Rarity.RARE, "RARE", "Heel Loop Mule"),
        Model(1103, Rarity.RARE, "RARE", "Toe Loop Sandal"),
        Model(1104, Rarity.RARE, "RARE", "Fisherman Cage Sandal"),
        Model(1105, Rarity.RARE, "RARE", "Zero Drop Wide Slip-On"),
        Model(1106, Rarity.RARE, "RARE", "Side Zip Commuter Runner"),
        Model(1107, Rarity.RARE, "RARE", "Split Heel Road Runner"),
        Model(1108, Rarity.RARE, "RARE", "Lug Trail Runner"),
        Model(1109, Rarity.RARE, "RARE", "Strap Road Runner"),
        Model(1110, Rarity.RARE, "RARE", "Amphibious Runner"),
        Model(1111, Rarity.RARE, "RARE", "Wide Toe Road Runner"),
        Model(1112, Rarity.RARE, "RARE", "Stability Road Runner"),
        Model(1113, Rarity.RARE, "RARE", "Strap Recovery Clog"),
        Model(1114, Rarity.RARE, "RARE", "Retro Canvas Jogger"),
        Model(1115, Rarity.RARE, "RARE", "Webbing Trail Runner"),
        Model(1116, Rarity.RARE, "RARE", "Rocker Walk Jogger"),
        Model(1117, Rarity.RARE, "RARE", "Bungee Lace Runner"),
        Model(1118, Rarity.RARE, "RARE", "Dial Road Runner"),
        Model(1119, Rarity.RARE, "RARE", "Tabi Split Toe Jogger"),
        Model(1120, Rarity.RARE, "RARE", "Packable Flex Runner"),
        Model(1201, Rarity.EPIC, "EPIC", "Knit Wrap Runner"),
        Model(1202, Rarity.EPIC, "EPIC", "Stability Daily Trainer"),
        Model(1203, Rarity.EPIC, "EPIC", "Tempo Rocker Trainer"),
        Model(1204, Rarity.EPIC, "EPIC", "Rock Guard Trail Runner"),
        Model(1205, Rarity.EPIC, "EPIC", "Long Run Cushion Trainer"),
        Model(1206, Rarity.EPIC, "EPIC", "Road-Trail Crossover Trainer"),
        Model(1207, Rarity.EPIC, "EPIC", "Rock Plate Trail Trainer"),
        Model(1208, Rarity.EPIC, "EPIC", "Track Interval Trainer"),
        Model(1209, Rarity.EPIC, "EPIC", "Reflective Night Trainer"),
        Model(1210, Rarity.EPIC, "EPIC", "Wet Road Trainer"),
        Model(1211, Rarity.EPIC, "EPIC", "Low Drop Form Trainer"),
        Model(1212, Rarity.EPIC, "EPIC", "Low Collar Gaiter Trail Runner"),
        Model(1213, Rarity.EPIC, "EPIC", "Heel Cushion Pod Trainer"),
        Model(1214, Rarity.EPIC, "EPIC", "External Heel Guide Trainer"),
        Model(1215, Rarity.EPIC, "EPIC", "Winter Microgrip Trainer"),
        Model(1216, Rarity.EPIC, "EPIC", "Forefoot Hill Repeat Trainer"),
        Model(1217, Rarity.EPIC, "EPIC", "Arch Bridge Road Trainer"),
        Model(1218, Rarity.EPIC, "EPIC", "Polymer Plate Tempo Trainer"),
        Model(1219, Rarity.EPIC, "EPIC", "Rocker Distance Trainer"),
        Model(1220, Rarity.EPIC, "EPIC", "Wide Toe Flex Trainer"),
        Model(1301, Rarity.LEGENDARY, "LEGENDARY", "Carbon Marathon Training Shoe"),
        Model(1302, Rarity.LEGENDARY, "LEGENDARY", "Stability Trainer"),
        Model(1303, Rarity.LEGENDARY, "LEGENDARY", "Carbon Rockplate Trail Runner"),
        Model(1304, Rarity.LEGENDARY, "LEGENDARY", "Light Midcut Trail Runner"),
        Model(1305, Rarity.LEGENDARY, "LEGENDARY", "Ultradistance Wave Cushion Trainer"),
        Model(1306, Rarity.LEGENDARY, "LEGENDARY", "Forefoot Marathon Interval Trainer"),
        Model(1307, Rarity.LEGENDARY, "LEGENDARY", "Asymmetric TPU Hill Trainer"),
        Model(1308, Rarity.LEGENDARY, "LEGENDARY", "Desert Trail Runner"),
        Model(1309, Rarity.LEGENDARY, "LEGENDARY", "Heel Cage Stability Trainer"),
        Model(1310, Rarity.LEGENDARY, "LEGENDARY", "Full-Length Curve Interval Trainer"),
        Model(1311, Rarity.LEGENDARY, "REDLINE", "Redline 100m Sprint Spike"),
        Model(1312, Rarity.LEGENDARY, "REDLINE", "Redline 800m Middle Distance Spike"),
        Model(1313, Rarity.LEGENDARY, "REDLINE", "Redline 5000m Distance Spike"),
        Model(1314, Rarity.LEGENDARY, "REDLINE", "Redline 5K·10K Racing Flat"),
        Model(1315, Rarity.LEGENDARY, "REDLINE", "Redline Marathon Carbon Super Shoe"),
        Model(1316, Rarity.LEGENDARY, "REDLINE", "Redline Trail Race Shoe"),
        Model(1317, Rarity.LEGENDARY, "REDLINE", "Redline Triathlon Transition Racer"),
        Model(1318, Rarity.LEGENDARY, "REDLINE", "Redline Steeplechase Drain Spike"),
        Model(1319, Rarity.LEGENDARY, "REDLINE", "Redline Half Marathon Road Racer"),
        Model(1320, Rarity.LEGENDARY, "REDLINE", "Redline Cross Country Spike"),
        Model(1321, Rarity.LEGENDARY, "FINISH", "Finish Championship Marathon Super Shoe"),
        Model(1322, Rarity.LEGENDARY, "FINISH", "Finish Elite 100m Sprint Spike"),
        Model(1323, Rarity.LEGENDARY, "FINISH", "Finish Elite Middle Distance Spike"),
        Model(1324, Rarity.LEGENDARY, "FINISH", "Finish Ultratrail Championship Racer"),
        Model(1325, Rarity.LEGENDARY, "FINISH", "Finish Championship 5K·10K Road Flat"),
        Model(1326, Rarity.LEGENDARY, "FINISH", "Finish Championship Cross Country Spike"),
        Model(1327, Rarity.LEGENDARY, "FINISH", "Finish Championship Steeplechase Spike"),
        Model(1328, Rarity.LEGENDARY, "FINISH", "Finish Championship 400m Sprint Spike"),
        Model(1329, Rarity.LEGENDARY, "FINISH", "Finish Championship 800m Middle Distance Spike"),
        Model(1330, Rarity.LEGENDARY, "FINISH", "Finish Championship Half Marathon Carbon Racer"),
    )

    private val byId: Map<Int, Model> = models.associateBy { it.id }

    fun of(id: Int?): Model? = id?.let(byId::get)
}
