package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stepup.android.R

// tools/gen_shoe_catalog.py 가 design/shoes-2026-09/catalog.json 으로 만든 파일 — 손으로 고치지 않는다.

/** 새 도감 신발 그림 — 없는 번호는 null(예전 속성 그림을 쓴다) */
@DrawableRes
fun shoeModelImageRes(id: Int): Int? = when (id) {
    1101 -> R.drawable.shoe_1101
    1102 -> R.drawable.shoe_1102
    1103 -> R.drawable.shoe_1103
    1104 -> R.drawable.shoe_1104
    1105 -> R.drawable.shoe_1105
    1106 -> R.drawable.shoe_1106
    1107 -> R.drawable.shoe_1107
    1108 -> R.drawable.shoe_1108
    1109 -> R.drawable.shoe_1109
    1110 -> R.drawable.shoe_1110
    1111 -> R.drawable.shoe_1111
    1112 -> R.drawable.shoe_1112
    1113 -> R.drawable.shoe_1113
    1114 -> R.drawable.shoe_1114
    1115 -> R.drawable.shoe_1115
    1116 -> R.drawable.shoe_1116
    1117 -> R.drawable.shoe_1117
    1118 -> R.drawable.shoe_1118
    1119 -> R.drawable.shoe_1119
    1120 -> R.drawable.shoe_1120
    1201 -> R.drawable.shoe_1201
    1202 -> R.drawable.shoe_1202
    1203 -> R.drawable.shoe_1203
    1204 -> R.drawable.shoe_1204
    1205 -> R.drawable.shoe_1205
    1206 -> R.drawable.shoe_1206
    1207 -> R.drawable.shoe_1207
    1208 -> R.drawable.shoe_1208
    1209 -> R.drawable.shoe_1209
    1210 -> R.drawable.shoe_1210
    1211 -> R.drawable.shoe_1211
    1212 -> R.drawable.shoe_1212
    1213 -> R.drawable.shoe_1213
    1214 -> R.drawable.shoe_1214
    1215 -> R.drawable.shoe_1215
    1216 -> R.drawable.shoe_1216
    1217 -> R.drawable.shoe_1217
    1218 -> R.drawable.shoe_1218
    1219 -> R.drawable.shoe_1219
    1220 -> R.drawable.shoe_1220
    1301 -> R.drawable.shoe_1301
    1302 -> R.drawable.shoe_1302
    1303 -> R.drawable.shoe_1303
    1304 -> R.drawable.shoe_1304
    1305 -> R.drawable.shoe_1305
    1306 -> R.drawable.shoe_1306
    1307 -> R.drawable.shoe_1307
    1308 -> R.drawable.shoe_1308
    1309 -> R.drawable.shoe_1309
    1310 -> R.drawable.shoe_1310
    1311 -> R.drawable.shoe_1311
    1312 -> R.drawable.shoe_1312
    1313 -> R.drawable.shoe_1313
    1314 -> R.drawable.shoe_1314
    1315 -> R.drawable.shoe_1315
    1316 -> R.drawable.shoe_1316
    1317 -> R.drawable.shoe_1317
    1318 -> R.drawable.shoe_1318
    1319 -> R.drawable.shoe_1319
    1320 -> R.drawable.shoe_1320
    1321 -> R.drawable.shoe_1321
    1322 -> R.drawable.shoe_1322
    1323 -> R.drawable.shoe_1323
    1324 -> R.drawable.shoe_1324
    1325 -> R.drawable.shoe_1325
    1326 -> R.drawable.shoe_1326
    1327 -> R.drawable.shoe_1327
    1328 -> R.drawable.shoe_1328
    1329 -> R.drawable.shoe_1329
    1330 -> R.drawable.shoe_1330
    else -> null
}

/** 새 도감 신발 이름 — 없는 번호는 null */
@StringRes
fun shoeModelNameRes(id: Int): Int? = when (id) {
    1101 -> R.string.shoe_1101
    1102 -> R.string.shoe_1102
    1103 -> R.string.shoe_1103
    1104 -> R.string.shoe_1104
    1105 -> R.string.shoe_1105
    1106 -> R.string.shoe_1106
    1107 -> R.string.shoe_1107
    1108 -> R.string.shoe_1108
    1109 -> R.string.shoe_1109
    1110 -> R.string.shoe_1110
    1111 -> R.string.shoe_1111
    1112 -> R.string.shoe_1112
    1113 -> R.string.shoe_1113
    1114 -> R.string.shoe_1114
    1115 -> R.string.shoe_1115
    1116 -> R.string.shoe_1116
    1117 -> R.string.shoe_1117
    1118 -> R.string.shoe_1118
    1119 -> R.string.shoe_1119
    1120 -> R.string.shoe_1120
    1201 -> R.string.shoe_1201
    1202 -> R.string.shoe_1202
    1203 -> R.string.shoe_1203
    1204 -> R.string.shoe_1204
    1205 -> R.string.shoe_1205
    1206 -> R.string.shoe_1206
    1207 -> R.string.shoe_1207
    1208 -> R.string.shoe_1208
    1209 -> R.string.shoe_1209
    1210 -> R.string.shoe_1210
    1211 -> R.string.shoe_1211
    1212 -> R.string.shoe_1212
    1213 -> R.string.shoe_1213
    1214 -> R.string.shoe_1214
    1215 -> R.string.shoe_1215
    1216 -> R.string.shoe_1216
    1217 -> R.string.shoe_1217
    1218 -> R.string.shoe_1218
    1219 -> R.string.shoe_1219
    1220 -> R.string.shoe_1220
    1301 -> R.string.shoe_1301
    1302 -> R.string.shoe_1302
    1303 -> R.string.shoe_1303
    1304 -> R.string.shoe_1304
    1305 -> R.string.shoe_1305
    1306 -> R.string.shoe_1306
    1307 -> R.string.shoe_1307
    1308 -> R.string.shoe_1308
    1309 -> R.string.shoe_1309
    1310 -> R.string.shoe_1310
    1311 -> R.string.shoe_1311
    1312 -> R.string.shoe_1312
    1313 -> R.string.shoe_1313
    1314 -> R.string.shoe_1314
    1315 -> R.string.shoe_1315
    1316 -> R.string.shoe_1316
    1317 -> R.string.shoe_1317
    1318 -> R.string.shoe_1318
    1319 -> R.string.shoe_1319
    1320 -> R.string.shoe_1320
    1321 -> R.string.shoe_1321
    1322 -> R.string.shoe_1322
    1323 -> R.string.shoe_1323
    1324 -> R.string.shoe_1324
    1325 -> R.string.shoe_1325
    1326 -> R.string.shoe_1326
    1327 -> R.string.shoe_1327
    1328 -> R.string.shoe_1328
    1329 -> R.string.shoe_1329
    1330 -> R.string.shoe_1330
    else -> null
}
