# Source-backed screen inventory

Source inventory, not completion proof. A function may serve multiple routes/states. All rows require actual runtime review.

33 registered navigation routes; 34 screen functions; 18 overlay declarations. These counts are different measures, not completed screens.

## Routes

| Route | Parent tab | Chrome | Implementation | Verification |
|---|---|---|---|---|
| Screen.Run.route | Run | Main | pending | pending |
| Screen.Customize.route | Customize | Main | pending | pending |
| Routes.RUNNER_MARKET | Customize | Detail | pending | pending |
| Screen.Community.route | Community | Main | pending | pending |
| Routes.MAP | Community | Detail | pending | pending |
| Routes.ITEMS | Customize | Detail | pending | pending |
| Routes.MARKET_MODEL | Customize | Detail | pending | pending |
| Routes.NEWS | Run | Detail | pending | pending |
| Routes.EVENTS | Run | Detail | pending | pending |
| Screen.Profile.route | Profile | Main | pending | pending |
| Routes.RUN_ROUTE | Run | Focus | pending | pending |
| Routes.COURSES | Run | Detail | pending | pending |
| Routes.WALLET | Profile | Detail | pending | pending |
| Routes.NOTIFICATIONS | Profile | Detail | pending | pending |
| Routes.ACHIEVEMENTS | Profile | Detail | pending | pending |
| Routes.ANALYTICS | Profile | Detail | pending | pending |
| Routes.HISTORY_MAP | Profile | Detail | pending | pending |
| Routes.SETTINGS_NOTIFICATIONS | Profile | Detail | pending | pending |
| Routes.SETTINGS_PRIVACY | Profile | Detail | pending | pending |
| Routes.SETTINGS_SUPPORT | Profile | Detail | pending | pending |
| Routes.SETTINGS_CONNECTED | Profile | Detail | pending | pending |
| Routes.SETTINGS_EXPERIENCE | Profile | Detail | pending | pending |
| Routes.SETTINGS_THEME | Profile | Detail | pending | pending |
| Routes.SETTINGS_LANGUAGE | Profile | Detail | pending | pending |
| Routes.SNEAKER | Customize | Detail | pending | pending |
| Routes.LOBBY | Community | Detail | pending | pending |
| Routes.SNEAKER_DEX | Customize | Detail | pending | pending |
| Routes.RANKING | Community | Detail | pending | pending |
| Routes.CREW_CREATE | Community | Form | pending | pending |
| Routes.CREW_BOARD | Community | Detail | pending | pending |
| Routes.FLASH_DETAIL | Community | Detail | pending | pending |
| Routes.FLASH_LOBBY | Community | Detail | pending | pending |
| Routes.POST_COMPOSE | Community | Form | pending | pending |

## Screen functions

| Function | Source | Implementation | Verification |
|---|---|---|---|
| CommunityScreen | app/src/main/java/com/stepup/android/ui/screens/community/CommunityScreen.kt:99 | pending | pending |
| CrewBoardScreen | app/src/main/java/com/stepup/android/ui/screens/community/CrewBoardScreen.kt:60 | pending | pending |
| CrewCreateScreen | app/src/main/java/com/stepup/android/ui/screens/community/CrewCreateScreen.kt:44 | pending | pending |
| FlashRunDetailScreen | app/src/main/java/com/stepup/android/ui/screens/community/FlashRunDetailScreen.kt:87 | pending | pending |
| PartyLobbyScreen | app/src/main/java/com/stepup/android/ui/screens/community/PartyLobbyScreen.kt:94 | pending | pending |
| PostComposeScreen | app/src/main/java/com/stepup/android/ui/screens/community/PostComposeScreen.kt:51 | pending | pending |
| RankingScreen | app/src/main/java/com/stepup/android/ui/screens/community/RankingScreen.kt:77 | pending | pending |
| CustomizeScreen | app/src/main/java/com/stepup/android/ui/screens/customize/CustomizeScreen.kt:105 | pending | pending |
| RunnerMarketScreen | app/src/main/java/com/stepup/android/ui/screens/customize/RunnerMarketScreen.kt:94 | pending | pending |
| EventsScreen | app/src/main/java/com/stepup/android/ui/screens/events/EventsScreen.kt:105 | pending | pending |
| NewsScreen | app/src/main/java/com/stepup/android/ui/screens/events/NewsScreen.kt:84 | pending | pending |
| HomeScreen | app/src/main/java/com/stepup/android/ui/screens/home/HomeScreen.kt:93 | pending | pending |
| ItemsScreen | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:102 | pending | pending |
| SneakerDetailScreen | app/src/main/java/com/stepup/android/ui/screens/items/SneakerDetailScreen.kt:69 | pending | pending |
| SneakerDexScreen | app/src/main/java/com/stepup/android/ui/screens/items/SneakerDexScreen.kt:78 | pending | pending |
| LoginScreen | app/src/main/java/com/stepup/android/ui/screens/login/LoginScreen.kt:75 | pending | pending |
| MapScreen | app/src/main/java/com/stepup/android/ui/screens/map/MapScreen.kt:85 | pending | pending |
| MarketModelScreen | app/src/main/java/com/stepup/android/ui/screens/market/MarketModelScreen.kt:79 | pending | pending |
| NotificationsScreen | app/src/main/java/com/stepup/android/ui/screens/notifications/NotificationsScreen.kt:150 | pending | pending |
| AchievementsScreen | app/src/main/java/com/stepup/android/ui/screens/profile/AchievementsScreen.kt:184 | pending | pending |
| AnalyticsScreen | app/src/main/java/com/stepup/android/ui/screens/profile/AnalyticsScreen.kt:133 | pending | pending |
| HistoryMapScreen | app/src/main/java/com/stepup/android/ui/screens/profile/HistoryMapScreen.kt:121 | pending | pending |
| ProfileScreen | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:141 | pending | pending |
| WalletScreen | app/src/main/java/com/stepup/android/ui/screens/rewards/RewardsScreen.kt:88 | pending | pending |
| ConnectedAccountsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ConnectedAccountsScreen.kt:62 | pending | pending |
| ExperienceSettingsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ExperienceSettingsScreen.kt:25 | pending | pending |
| LanguageScreen | app/src/main/java/com/stepup/android/ui/screens/settings/LanguageScreen.kt:75 | pending | pending |
| NotificationSettingsScreen | app/src/main/java/com/stepup/android/ui/screens/settings/NotificationSettingsScreen.kt:61 | pending | pending |
| PrivacyScreen | app/src/main/java/com/stepup/android/ui/screens/settings/PrivacyScreen.kt:50 | pending | pending |
| SupportScreen | app/src/main/java/com/stepup/android/ui/screens/settings/SupportScreen.kt:55 | pending | pending |
| ThemeScreen | app/src/main/java/com/stepup/android/ui/screens/settings/ThemeScreen.kt:75 | pending | pending |
| SplashScreen | app/src/main/java/com/stepup/android/ui/screens/splash/SplashScreen.kt:37 | pending | pending |
| CourseHubScreen | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:92 | pending | pending |
| RunScreen | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:153 | pending | pending |

## Dialog / sheet / menu declarations

| Owner | Type | Source | Verification |
|---|---|---|---|
| ReportDialogHost | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/community/BoardParts.kt:93 | pending |
| CommentSheet | Dialog | app/src/main/java/com/stepup/android/ui/screens/community/CommentSheet.kt:150 | pending |
| FlashMembersDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/community/FlashRunDetailScreen.kt:432 | pending |
| CustomizeScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/customize/CustomizeScreen.kt:242 | pending |
| InviteDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/events/EventsScreen.kt:434 | pending |
| HomeScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/home/HomeScreen.kt:173 | pending |
| ItemsScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:532 | pending |
| CopiesDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/items/ItemsScreen.kt:689 | pending |
| MarketDialog | Dialog | app/src/main/java/com/stepup/android/ui/screens/market/MarketModelScreen.kt:484 | pending |
| GoalDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:958 | pending |
| ProfileEditDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/profile/ProfileScreen.kt:1068 | pending |
| ConnectedAccountsScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/settings/ConnectedAccountsScreen.kt:71 | pending |
| CourseHubScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:263 | pending |
| CourseRankingDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/walk/CourseHubScreen.kt:845 | pending |
| RunScreen | ModalBottomSheet | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:527 | pending |
| RunScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:565 | pending |
| RunScreen | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:597 | pending |
| SaveCourseDialog | AlertDialog | app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt:1365 | pending |

## State review

For every route: initializing, ready/data, empty, loading, error/retry, permission denied, signed out, signed in, offline/reconnect, restore/back/relaunch, large font, light/dark.
Mark genuinely inapplicable states with a reason during review. Source detection is not runtime evidence.

Startup: system launch, initialization, logo reveal, login, first-use tour.
Running: ready, permission denied, acquiring GPS, active, paused, confirm finish, result, pending server reward, reward rejected, restored active session.
