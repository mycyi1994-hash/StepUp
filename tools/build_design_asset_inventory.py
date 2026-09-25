"""Expand StepUp's current UI/catalog into a reviewable design-asset checklist.

This inventories logical art packages. A package can export multiple transparent
layers; it is deliberately not described as one finished PNG.
"""

from collections import Counter
from pathlib import Path
import csv
import re


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/redesign"
AVATAR = ROOT / "app/src/main/java/com/stepup/android/domain/Avatar.kt"
ART = ROOT / "app/src/main/java/com/stepup/android/domain/AvatarArt.kt"
SHOES = ROOT / "app/src/main/java/com/stepup/android/domain/Sneaker.kt"
RES = ROOT / "app/src/main/res/drawable-nodpi"


def one(pattern: str, content: str, source: str) -> str:
    match = re.search(pattern, content, re.S)
    if match is None:
        raise ValueError(f"Cannot resolve {source}; inventory would be incomplete")
    return match.group(1)


def catalog():
    avatar, art, shoes = (p.read_text(encoding="utf-8") for p in (AVATAR, ART, SHOES))
    gender_body = one(r"enum class AvatarGender[^\{]*\{(.*?)\n\s*companion object", avatar, "genders")
    genders = re.findall(r"\b(MALE|FEMALE)\(\"", gender_body)
    poses = [p.strip() for p in one(r"enum class AvatarPose\s*\{([^}]+)\}", art, "poses").split(",")]

    all_names = [x.strip() for x in one(r"val ALL = listOf\(([^)]+)\)", avatar, "outfits").split(",")]
    studio_names = [x.strip() for x in one(r"val STUDIO = listOf\(([^)]+)\)", avatar, "studio outfits").split(",")]
    preview_names = [x.strip() for x in one(r"val PREVIEWABLE = listOf\(([^)]+)\)", avatar, "preview outfits").split(",")]
    if len(genders) != 2 or len(poses) != 4 or len(all_names) != 6 or len(studio_names) != 3:
        raise ValueError("Character catalog changed; update the asset inventory rules")
    if set(preview_names) != set(all_names + studio_names):
        raise ValueError("A previewable outfit has no asset inventory entry")

    outfit_ids = []
    for name in all_names + studio_names:
        value = one(r"val " + re.escape(name) + r"\s*=\s*(?:Outfit\(|STARTER_HOODIE\.copy\()\s*id\s*=\s*([^,]+),", avatar, name).strip()
        outfit_ids.append("starter_hoodie" if value == "BASE_ID" else value.strip('"'))
    if len(set(outfit_ids)) != 9:
        raise ValueError("Outfit IDs are duplicated")

    shoe_codes = []
    for faction, prefix in (("FIRE", "FIR"), ("WATER", "WAT"), ("LIGHTNING", "LIT"), ("WIND", "WND")):
        block = one(r"Faction\." + faction + r" to listOf\((.*?)\n\s*\),", shoes, faction)
        indexes = [int(x) for x in re.findall(r"row\((\d+),", block)]
        if indexes != list(range(1, 14)):
            raise ValueError(f"Incomplete {faction} shoe catalog: {indexes}")
        shoe_codes += [f"{prefix}-{i:03d}" for i in indexes]
    if len(shoe_codes) != 52:
        raise ValueError("Shoe catalog changed")
    return genders, poses, outfit_ids, ["SHOES-BASE"] + shoe_codes


def resource(stem: str) -> str:
    matches = sorted(RES.glob(stem + ".*"))
    if len(matches) != 1:
        raise ValueError(f"Expected one app drawable for {stem}; found {len(matches)}")
    return matches[0].relative_to(ROOT).as_posix()


def write_csv(path: Path, fields: list[str], rows: list[dict]):
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def build():
    genders, poses, outfits, shoe_codes = catalog()
    gender_labels = {"MALE": "runo", "FEMALE": "lumi"}
    items: list[dict] = []
    cap_bindings: list[dict] = []

    def add(family, gender, pose, variant, status, evidence, specification, usage):
        token = f"{gender.lower()}-{pose.lower()}-{family}-{variant.lower().replace('_', '-')}"
        items.append(dict(id=token, family=family, gender=gender, pose=pose,
                          variant=variant, status=status, evidence=evidence,
                          specification=specification, usage=usage))
        return token

    for gender in genders:
        label = gender_labels[gender]
        for pose in poses:
            body_pilot = ROOT / "design/redesign-2026-09/assets/modular/lumi-idle-base-pilot.png"
            add("body", gender, pose, "base", "REWORK_PILOT" if gender == "FEMALE" and pose == "IDLE" else "NEW",
                body_pilot.relative_to(ROOT).as_posix() if gender == "FEMALE" and pose == "IDLE" and body_pilot.exists() else "",
                "1024x1536 transparent aligned body/face/eyes/skin; rear hair/arms and front hair/hands/occlusion layers; no outfit, cap or shoe",
                "all character scenes")
            for outfit in outfits:
                pilot_name = {"STUDIO-PINK": "lumi-idle-outfit-pink-pilot.png", "STUDIO-LAVENDER": "lumi-idle-outfit-lavender-pilot.png"}.get(outfit)
                pilot = ROOT / "design/redesign-2026-09/assets/modular" / (pilot_name or "")
                use_pilot = gender == "FEMALE" and pose == "IDLE" and bool(pilot_name) and pilot.exists()
                add("outfit", gender, pose, outfit, "REWORK_PILOT" if use_pilot else "NEW",
                    pilot.relative_to(ROOT).as_posix() if use_pilot else "",
                    "same-frame upper, shorts and socks; preserve sleeve/hand overlap with rear/front garment or masks; no cap or shoes",
                    "home, run, wardrobe, challenge, result, profile, market preview")

            # RUNO's five sale outfits currently share the base cap design/color.
            # LUMI's five sale caps differ by outfit (lumi/CAP_RULES.md).
            cap_master = [outfits[0], *outfits[6:]] if gender == "MALE" else outfits
            master_ids = {}
            for cap in cap_master:
                pilot = ROOT / "design/redesign-2026-09/assets/modular/lumi-idle-cap-pink-pilot.png"
                use_pilot = gender == "FEMALE" and pose == "IDLE" and cap == "STUDIO-PINK" and pilot.exists()
                master_ids[cap] = add("cap", gender, pose, cap, "REWORK_PILOT" if use_pilot else "NEW",
                    pilot.relative_to(ROOT).as_posix() if use_pilot else "",
                    "same-frame cap, brim and UP logo; rear/front or hair occlusion where required; selected by outfit ID",
                    "all equipped-character scenes")
            for outfit in outfits:
                master = outfits[0] if gender == "MALE" and outfit in outfits[:6] else outfit
                cap_bindings.append(dict(gender=gender, pose=pose, outfit=outfit, cap_asset=master_ids[master]))

            for code in shoe_codes:
                pilot = ROOT / "design/redesign-2026-09/assets/modular/lumi-idle-shoes-teal-pilot.png"
                use_pilot = gender == "FEMALE" and pose == "IDLE" and code == "WND-010" and pilot.exists()
                add("worn_shoes", gender, pose, code, "REWORK_PILOT" if use_pilot else "NEW",
                    pilot.relative_to(ROOT).as_posix() if use_pilot else "",
                    "same-frame left/right worn pair, sole and foot contact; rear/front and occlusion where required; not the product thumbnail",
                    "all equipped-character scenes")

    product_rows = []
    for code in shoe_codes[1:]:
        prefix = {"FIR": "fire", "WAT": "water", "LIT": "lightning", "WND": "wind"}[code[:3]]
        stem = f"sneaker_{prefix}_{int(code[-3:]):02d}"
        product_rows.append(dict(id=code, kind="sneaker_product", status="RENDER_HIGH_RES",
                                 existing=resource(stem), requirement="transparent product cutout; preserve shared scale; inspect light/dark edges; 340x209 current source"))
    for code in ("CLO-001", "CLO-002", "CLO-003", "CLO-004", "CLO-005"):
        for gender, prefix in (("MALE", "outfit_clo_"), ("FEMALE", "outfit_lum_clo_")):
            stem = prefix + code[-3:]
            product_rows.append(dict(id=f"{gender}-{code}", kind="outfit_product", status="RENDER_HIGH_RES",
                                     existing=resource(stem), requirement="transparent product cutout matching worn garment/cap; existing width under 310px"))
    for stem in ("outfit_runo_base", "outfit_lumi_base", "outfit_studio_pink", "outfit_studio_lavender", "outfit_studio_olive"):
        product_rows.append(dict(id=stem, kind="outfit_product", status="REUSE_VISUAL_REVIEW",
                                 existing=resource(stem), requirement="review against new worn layers and both themes; 1254px existing art"))

    scenes = {
        "Night": "scene_riverside_night", "Sunset": "scene_riverside_sunset",
        "Wardrobe": "scene_wardrobe_terrace", "HomeBlueNight": "scene_home_blue_night",
        "HomeDawn": "scene_home_dawn", "RunNight": "scene_run_night",
        "RunSunset": "scene_run_sunset", "Terrace": "scene_terrace_stage",
    }
    scene_rows = [dict(id=key, status="REUSE_CROP_LIGHT_DARK_REVIEW", existing=resource(stem),
                       review="ground/contact, safe crop, title contrast and character light in every allowed scene")
                  for key, stem in scenes.items()]
    scene_rows[-1]["status"] = "AVAILABLE_NOT_ACTIVE"
    scene_rows += [dict(id="ProfileBench", status="REUSE_CONTACT_REVIEW", existing=resource("prop_profile_bench_v2"),
                        review="seat/front overlap for RUNO and LUMI, all outfits and both profile backgrounds")]

    backlog = ROOT / "docs/redesign/DESIGN-SCREEN-BACKLOG-KO.md"
    screen_dir = ROOT / "design/redesign-2026-09/screens"
    status_text = (screen_dir / "SCREEN-STATUS-KO.md").read_text(encoding="utf-8")
    mockup_rows = []
    mapped_paths = set()
    for line in status_text.splitlines():
        match = re.match(r"^\| \[([^]]+)\]\(([^)]+\.png)\) \| ([^|]+) \|", line)
        if not match:
            continue
        title, target, mapped_state = match.groups()
        path = (screen_dir / target).resolve()
        if not path.is_file() or path in mapped_paths:
            raise ValueError(f"Duplicate or missing screen draft: {target}")
        mapped_paths.add(path)
        mockup_rows.append(dict(path=path.relative_to(ROOT).as_posix(), title=title,
                                work_item_or_state=mapped_state.strip(), status="CURRENT_DRAFT_PENDING_NATIVE_REVIEW"))
    previous_drafts = [p for p in screen_dir.glob("*.png") if p.resolve() not in mapped_paths and p.name != "component-board.png"]
    for path in sorted(previous_drafts):
        mockup_rows.append(dict(path=path.relative_to(ROOT).as_posix(), title=path.stem,
                                work_item_or_state="superseded by current status-table draft", status="SUPERSEDED_REFERENCE"))
    board = screen_dir / "component-board.png"
    if not board.is_file():
        raise ValueError("Missing shared component board")
    mockup_rows.append(dict(path=board.relative_to(ROOT).as_posix(), title="component board",
                            work_item_or_state="M01-M18", status="SHARED_COMPONENT_DRAFT_PENDING_NATIVE_REVIEW"))
    screen_scene_ids = {
        "screen-00": "HomeBlueNight; HomeDawn; Night", "screen-01": "RunNight; RunSunset",
        "screen-02": "HomeDawn", "screen-04": "HomeBlueNight; RunSunset; RunNight",
        "screen-05": "HomeBlueNight; HomeDawn", "screen-22": "Sunset (hero)",
        "screen-24": "Night", "screen-28": "Wardrobe; Sunset",
        "screen-29": "RunNight (hero)", "screen-33": "Night (reveal)",
        "screen-34": "RunNight; RunSunset", "screen-35": "RunNight; RunSunset",
        "extra-challenge-weekly": "RunSunset", "extra-challenge-night": "RunNight",
        "extra-profile-settings": "HomeBlueNight; HomeDawn",
        "extra-customize-shoes": "Wardrobe; Sunset",
        "extra-community-meetups": "HomeDawn",
        "extra-market-outfits": "RunNight (hero)", "extra-market-shoes": "RunNight (hero)",
        "launch-logo-reveal": "Night (reveal)",
        "launch-preparation-error": "Night (hidden until reveal)",
        "guide-00": "HomeBlueNight; HomeDawn; Night (underlying home)",
        "guide-01": "Wardrobe; Sunset (underlying wardrobe)",
        "guide-02": "HomeDawn (underlying community)",
        "guide-03": "HomeBlueNight; HomeDawn (underlying profile)",
        "extra-home-details": "HomeBlueNight; HomeDawn; Night (underlying home)",
    }
    screen_pose_ids = {
        "screen-00": "IDLE", "screen-01": "IDLE", "screen-04": "IDLE", "screen-05": "SIT or fallback IDLE",
        "screen-28": "IDLE", "screen-29": "RUN", "screen-34": "RUN", "screen-35": "CHEER or IDLE",
        "extra-challenge-weekly": "IDLE", "extra-challenge-night": "IDLE",
        "extra-profile-settings": "SIT or fallback IDLE", "extra-customize-shoes": "IDLE",
        "extra-market-outfits": "RUN", "extra-market-shoes": "RUN",
        "extra-home-details": "IDLE (underlying home)", "guide-00": "IDLE (underlying home)",
        "guide-01": "IDLE (underlying wardrobe)", "guide-03": "SIT or fallback IDLE (underlying profile)",
    }
    screen_rows = []
    for line in backlog.read_text(encoding="utf-8").splitlines():
        if not re.match(r"^\| (screen-|extra-|guide-|launch-)", line):
            continue
        parts = [x.strip() for x in line.strip("|").split("|")]
        ident, name, _, layout, modules, image_plan = parts[:6]
        matching_mockups = [row["path"] for row in mockup_rows
                            if row["status"] == "CURRENT_DRAFT_PENDING_NATIVE_REVIEW"
                            and re.search(r"(?<![A-Za-z0-9_-])" + re.escape(ident) + r"(?![A-Za-z0-9_-])",
                                          row["work_item_or_state"])]
        if not matching_mockups:
            raise ValueError(f"No current screen mockup mapping: {ident}")
        tags = set(re.findall(r"M\d{2}", modules))
        needed = []
        source_scene = {"screen-02", "extra-community-meetups", "screen-22",
                        "screen-29", "extra-market-outfits", "extra-market-shoes"}
        source_avatar = {"screen-29", "extra-market-outfits", "extra-market-shoes"}
        source_warmup = {"screen-02", "extra-community-meetups", "screen-24"}
        if "M15" in tags or ident in source_scene: needed.append("scene")
        if "M16" in tags or ident in source_avatar: needed.append("avatar_packages")
        if "M07" in tags: needed.append("product_art")
        if "M17" in tags: needed.append("live_map_tiles_and_vectors")
        if ident in ("screen-05", "extra-profile-settings"): needed.append("profile_bench")
        if ident in source_warmup: needed.append("community_warmup_existing")
        if not needed: needed.append("native_UI_and_existing_icons")
        screen_rows.append(dict(id=ident, screen=name, layout=layout, modules=modules,
                                asset_families="; ".join(needed), plan=image_plan,
                                scene_ids=screen_scene_ids.get(ident, "parent scene or native surface"),
                                character_pose=screen_pose_ids.get(ident, "none or inherited from parent"),
                                mockup_files="; ".join(matching_mockups),
                                mockup="EXISTS_DRAFT", native_visual_review="PENDING"))

    # Fail when a catalog/backlog/source change would silently make this list partial.
    counts = Counter(row["family"] for row in items)
    assert counts == {"body": 8, "outfit": 72, "cap": 52, "worn_shoes": 424}, counts
    assert len(items) == 556 and len(cap_bindings) == 72
    assert len(product_rows) == 67 and len(scene_rows) == 9
    assert len(screen_rows) == len({x["id"] for x in screen_rows}) == 86
    assert len(mapped_paths) == 90 and len(previous_drafts) == 13 and len(mockup_rows) == 104
    assert len({x["id"] for x in items}) == len(items)
    drawable_names = {p.stem for p in (ROOT / "app/src/main/res").rglob("*")
                      if p.is_file() and p.parent.name.startswith("drawable")}
    ui = ROOT / "app/src/main/java/com/stepup/android/ui"
    drawable_refs = {name for p in ui.rglob("*.kt") for name in
                     re.findall(r"R\.drawable\.([A-Za-z_][A-Za-z_0-9]*)", p.read_text(encoding="utf-8"))}
    missing_drawables = drawable_refs - drawable_names
    if missing_drawables:
        raise ValueError(f"Missing drawable references: {sorted(missing_drawables)}")

    # Audit every packaged visual/font file as well as the new-work checklist.
    # This catches one-off assets such as the Google sign-in mark or share logo.
    existing_rows = []
    existing_counts = Counter()
    for folder in sorted((ROOT / "app/src/main/res").iterdir()):
        if not folder.is_dir() or not folder.name.startswith(("drawable", "mipmap", "font")):
            continue
        for path in sorted(folder.iterdir()):
            if not path.is_file():
                continue
            name = path.stem
            if name.startswith("avatar_"):
                family, action = "legacy_full_avatar", "FALLBACK_UNTIL_MODULAR_REVIEW"
            elif name.startswith("sneaker_"):
                family, action = "sneaker_product", "RENDER_HIGH_RES"
            elif name.startswith("outfit_"):
                family = "outfit_product"
                action = "RENDER_HIGH_RES" if name.startswith(("outfit_clo_", "outfit_lum_clo_")) else "REUSE_VISUAL_REVIEW"
            elif name.startswith("scene_"):
                family, action = "scene", "REUSE_CROP_LIGHT_DARK_REVIEW" if name != "scene_terrace_stage" else "AVAILABLE_NOT_ACTIVE"
            elif name == "prop_profile_bench_v2":
                family, action = "profile_bench", "REUSE_CONTACT_REVIEW"
            elif name.startswith("logo_wordmark"):
                family, action = "wordmark", "REUSE_THEME_REVIEW"
            elif name == "brand_logo_white_on_blue":
                family, action = "run_share_logo", "REUSE_EXPORT_REVIEW"
            elif name == "community_warmup":
                family, action = "community_illustration", "REUSE_CROP_THEME_REVIEW"
            elif name == "google_g":
                family, action = "google_sign_in_mark", "REUSE_CONTRAST_REVIEW"
            elif name.startswith("ic_launcher"):
                family, action = "launcher_icon", "REUSE_SAFE_AREA_REVIEW"
            elif name == "ic_stat_walk":
                family, action = "notification_icon", "REUSE_LEGIBILITY_REVIEW"
            elif folder.name == "font":
                family, action = "font", "REUSE_FONT_SCALE_REVIEW"
            else:
                raise ValueError(f"Unclassified packaged visual resource: {path}")
            existing_counts[family] += 1
            existing_rows.append(dict(path=path.relative_to(ROOT).as_posix(), family=family, action=action))

    assert existing_counts == {
        "legacy_full_avatar": 127, "sneaker_product": 52, "outfit_product": 15,
        "scene": 8, "profile_bench": 1, "wordmark": 2, "run_share_logo": 1,
        "community_illustration": 1, "google_sign_in_mark": 1,
        "launcher_icon": 13, "notification_icon": 1, "font": 7,
    }, existing_counts

    OUT.mkdir(parents=True, exist_ok=True)
    write_csv(OUT / "ASSET-CHARACTER-REQUIREMENTS.csv",
              ["id", "family", "gender", "pose", "variant", "status", "evidence", "specification", "usage"], items)
    write_csv(OUT / "ASSET-CAP-BINDINGS.csv", ["gender", "pose", "outfit", "cap_asset"], cap_bindings)
    write_csv(OUT / "ASSET-PRODUCT-REQUIREMENTS.csv",
              ["id", "kind", "status", "existing", "requirement"], product_rows)
    write_csv(OUT / "ASSET-SCENE-REVIEW.csv", ["id", "status", "existing", "review"], scene_rows)
    write_csv(OUT / "ASSET-SCREEN-COVERAGE.csv",
              ["id", "screen", "layout", "modules", "asset_families", "plan", "scene_ids", "character_pose", "mockup_files", "mockup", "native_visual_review"], screen_rows)
    write_csv(OUT / "ASSET-EXISTING-RESOURCES.csv", ["path", "family", "action"], existing_rows)
    write_csv(OUT / "ASSET-MOCKUP-REVIEW.csv", ["path", "title", "work_item_or_state", "status"], mockup_rows)
    print(f"Character packages: {len(items)} {dict(counts)}; cap bindings {len(cap_bindings)}")
    print(f"Product assets: {len(product_rows)} (62 high-resolution redraws, 5 review); scenes/bench {len(scene_rows)} review")
    print(f"Screen/state coverage: {len(screen_rows)}")
    print(f"App UI drawable references: {len(drawable_refs)}; missing: {len(missing_drawables)}")
    print(f"Packaged visual/font files: {len(existing_rows)} {dict(existing_counts)}; unclassified: 0")
    print(f"Mockups: {len(mapped_paths)} current screen drafts, {len(previous_drafts)} superseded, one shared component board")


if __name__ == "__main__":
    build()
