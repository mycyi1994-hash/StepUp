# Source-backed screen inventory

Source inventory, not completion proof. A function may serve multiple routes/states. All rows require actual runtime review.

33 registered navigation routes; 34 screen functions; 20 overlay declarations. These counts are different measures, not completed screens.

## Routes

| Route | Parent tab | Chrome | Implementation | Verification |
|---|---|---|---|---|
| Screen.Run.route | Run | Main | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Screen.Customize.route | Customize | Main | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Routes.RUNNER_MARKET | Customize | Detail | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Screen.Community.route | Community | Main | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.MAP | Community | Detail | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |
| Routes.ITEMS | Customize | Detail | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Routes.MARKET_MODEL | Customize | Detail | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| Routes.NEWS | Run | Detail | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Routes.EVENTS | Run | Detail | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| Screen.Profile.route | Profile | Main | profile seated exact pink + WND-010 portraits; independent bench and saved random scenery; shared native controls; see assets/profile/README-KO.md | source checks and browser asset composition only; Android compile/capture and unit test execution pending; no APK requested |
| Routes.RUN_ROUTE | Run | Focus | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |
| Routes.COURSES | Run | Detail | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |
| Routes.WALLET | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.NOTIFICATIONS | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.ACHIEVEMENTS | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.ANALYTICS | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.HISTORY_MAP | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_NOTIFICATIONS | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_PRIVACY | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_SUPPORT | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_CONNECTED | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_EXPERIENCE | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_THEME | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SETTINGS_LANGUAGE | Profile | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SNEAKER | Customize | Detail | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| Routes.LOBBY | Community | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.SNEAKER_DEX | Customize | Detail | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| Routes.RANKING | Community | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.CREW_CREATE | Community | Form | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.CREW_BOARD | Community | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.FLASH_DETAIL | Community | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.FLASH_LOBBY | Community | Detail | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |
| Routes.POST_COMPOSE | Community | Form | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending; no APK requested |

## Screen functions

| Function | Source | Implementation | Verification |
|---|---|---|---|
| CommunityScreen | app/src/main/java/com/stepup/android/ui/screens/community/CommunityScreen.kt:86 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| CrewBoardScreen | app/src/main/java/com/stepup/android/ui/screens/community/CrewBoardScreen.kt:51 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| CrewCreateScreen | app/src/main/java/com/stepup/android/ui/screens/community/CrewCreateScreen.kt:38 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| FlashRunDetailScreen | app/src/main/java/com/stepup/android/ui/screens/community/FlashRunDetailScreen.kt:83 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| PartyLobbyScreen | app/src/main/java/com/stepup/android/ui/screens/community/PartyLobbyScreen.kt:80 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| PostComposeScreen | app/src/main/java/com/stepup/android/ui/screens/community/PostComposeScreen.kt:43 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| RankingScreen | app/src/main/java/com/stepup/android/ui/screens/community/RankingScreen.kt:73 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| CustomizeScreen | app/src/main/java/com/stepup/android/ui/screens/customize/CustomizeScreen.kt:100 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| RunnerMarketScreen | app/src/main/java/com/stepup/android/ui/screens/customize/RunnerMarketScreen.kt:93 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| EventsScreen | app/src/main/java/com/stepup/android/ui/screens/events/EventsScreen.kt:79 | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| NewsScreen | app/src/main/java/com/stepup/android/ui/screens/events/NewsScreen.kt:76 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| HomeScreen | app/src/main/java/com/stepup/android/ui/screens/home/HomeScreen.kt:84 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| ItemsScreen | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:91 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| SneakerDetailScreen | app/src/main/java/com/stepup/android/ui/screens/items/SneakerDetailScreen.kt:44 | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| SneakerDexScreen | app/src/main/java/com/stepup/android/ui/screens/items/SneakerDexScreen.kt:77 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| LoginScreen | app/src/main/java/com/stepup/android/ui/screens/login/LoginScreen.kt:67 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| MapScreen | app/src/main/java/com/stepup/android/ui/screens/map/MapScreen.kt:93 | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |
| MarketModelScreen | app/src/main/java/com/stepup/android/ui/screens/market/MarketModelScreen.kt:65 | detail/dialog source finish; see DETAIL-DIALOG-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/theme verification pending; no APK requested |
| NotificationsScreen | app/src/main/java/com/stepup/android/ui/screens/notifications/NotificationsScreen.kt:147 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| AchievementsScreen | app/src/main/java/com/stepup/android/ui/screens/profile/AchievementsScreen.kt:178 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| AnalyticsScreen | app/src/main/java/com/stepup/android/ui/screens/profile/AnalyticsScreen.kt:139 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| HistoryMapScreen | app/src/main/java/com/stepup/android/ui/screens/profile/HistoryMapScreen.kt:117 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| ProfileScreen | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:132 | profile seated exact pink + WND-010 portraits; independent bench and saved random scenery; shared native controls; see assets/profile/README-KO.md | source checks and browser asset composition only; Android compile/capture and unit test execution pending; no APK requested |
| WalletScreen | app/src/main/java/com/stepup/android/ui/screens/rewards/RewardsScreen.kt:74 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| ConnectedAccountsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ConnectedAccountsScreen.kt:57 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| ExperienceSettingsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ExperienceSettingsScreen.kt:25 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| LanguageScreen | app/src/main/java/com/stepup/android/ui/screens/settings/LanguageScreen.kt:39 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| NotificationSettingsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/NotificationSettingsScreen.kt:44 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| PrivacyScreen | app/src/main/java/com/stepup/android/ui/screens/settings/PrivacyScreen.kt:50 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| SupportScreen | app/src/main/java/com/stepup/android/ui/screens/settings/SupportScreen.kt:49 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| ThemeScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ThemeScreen.kt:39 | stage 7-9 source finish; see DESIGN-FINISH-2026-09-24-KO.md | source checks only; Android compile/capture pending |
| SplashScreen | app/src/main/java/com/stepup/android/ui/screens/splash/SplashScreen.kt:40 | home/catalog/news UI source finish; see HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/font/theme/IME verification pending; equipment art gaps remain; no APK requested |
| CourseHubScreen | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:86 | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |
| RunScreen | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:135 | run/map/course source finish; see RUN-MAP-COURSE-FINISH-2026-09-24-KO.md | source checks only; Android compile/runtime/IME/font/map interaction verification pending; no APK requested |

## Dialog / sheet / menu declarations

| Owner | Type | Source | Verification |
|---|---|---|---|
| ReportDialogHost | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/community/BoardParts.kt:95 | pending |
| CommentSheet | Dialog | app/src/main/java/com/stepup/android/ui/screens/community/CommentSheet.kt:148 | pending |
| FlashMembersDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/community/FlashRunDetailScreen.kt:385 | pending |
| CustomizeScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/customize/CustomizeScreen.kt:256 | pending |
| InviteDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/events/EventsScreen.kt:395 | pending |
| HomeScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/home/HomeScreen.kt:175 | pending |
| ItemsScreen | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:402 | pending |
| CopiesDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:537 | pending |
| SneakerDetailScreen | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/items/SneakerDetailScreen.kt:257 | pending |
| BidDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/market/MarketModelScreen.kt:307 | pending |
| AskDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/market/MarketModelScreen.kt:334 | pending |
| GoalDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:927 | pending |
| ProfileEditDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:976 | pending |
| ConnectedAccountsScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/settings/ConnectedAccountsScreen.kt:66 | pending |
| CourseHubScreen | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:257 | pending |
| CourseRankingDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:592 | pending |
| RunScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:417 | pending |
| RunScreen | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:454 | pending |
| RunScreen | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:478 | pending |
| SaveCourseDialog | DialogPanel | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:969 | pending |

## State review

For every route: initializing, ready/data, empty, loading, error/retry, permission denied, signed out, signed in, offline/reconnect, restore/back/relaunch, large font, light/dark.
Mark genuinely inapplicable states with a reason during review. Source detection is not runtime evidence.

Startup: system launch, initialization, logo reveal, login, first-use tour.
Running: ready, permission denied, acquiring GPS, active, paused, confirm finish, result, pending server reward, reward rejected, restored active session.
