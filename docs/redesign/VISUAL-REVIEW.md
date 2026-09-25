# Native visual review ledger

This file records actual image inspection and survives regeneration of the source inventory. A reviewed capture is not approval of all states, locales, sizes or live services.

## Evidence set

Revision 5c4fe1a, workflow 35929984701, API 34 gallery. Local root: `C:/Users/gana0/StepUp-captures/redesign-5c4fe1a/api-34-gallery/screen-gallery`.
The fixture renders a 390×844dp viewport at density 1.8 inside a larger device display, hence the surrounding white area. It uses seeded test data and a signed-out server state; its balances/posts are not production-user evidence. The final delivery needs a current, clearly labelled gallery and full device-size verification.

| Capture | Inspected state | Observation / action | Remaining evidence |
|---|---|---|---|
| screen-02 | Community, seeded meetup plus sign-in-required state | Shared header/nav are present, main action is clear. Auth banner and meetup card make the initial fold dense. | Signed-in/empty/offline states, enlarged text, actual meetup operations; compact screen card/CTA boundary. |
| screen-05 | Profile, equipped starter and no saved runs | Identity, two totals and three destinations are readable in this viewport. | Large-font navigation, long name, loading/error, account partition; approved seated character direction remains unimplemented. |
| screen-06 | Wallet, two seeded ledger rows | SUP unit sat above the amount baseline; small hero labels were low opacity. Aligned amount/unit baselines and made their text opaque. | New capture, large values/text, real server reconciliation, GIWA integration. |
| screen-10 | Notifications, goal and legacy reward | Legacy reward copy does not promise an unverified credit; challenge action visible. | All invite/error/empty/loading states and long text. |
| screen-11 | Sound/motion settings, sounds/haptics off | Three switches and explanation fit; preview is visibly disabled with sounds off. | Real sound/haptic behavior, system restrictions, larger text, return/reopen persistence. |
| screen-12 | Notification settings, server sync pending | Pending status and retry are visible, separate from local toggle values. | Server acknowledgement/delivery, system/channel denial and return, larger text. |
| screen-13 | Privacy settings | Found hardcoded permission checkmarks and missing location status. Replaced with OS readings, including precise/approximate location, resume refresh and app-settings access. | New capture, denied/approximate/granted states and actual settings round trip. |
| screen-16 | Language, Korean selected | List fits; explanatory copy falsely promised all user posts switch language. Revised all four locales to state user content may retain its original language. | Actual locale switch/recreation, all dialogs and dynamic content. |

## Shared controls: separate evidence

- 5c4fe1a large-font API 35 capture shows filter footer under system navigation despite passing clicks. API 34 shows full footer. The stronger bounds assertion subsequently catches the API 35 failure.
- 9be0c87 API 35 large-font capture confirms `decorFitsSystemWindows=false` alone did not resolve it. Upstream Dialog inset handling is missing from installed UI 1.7.6 source and present in UI 1.8.2 source; BOM 2025.06.00 selects UI/Foundation/Runtime 1.8.2 and Material3 1.3.2. [Upstream fix](https://android.googlesource.com/platform/frameworks/support/+/8365bb7470019e7cf7bfc046dda9cc8d978a8d23). Upgrade requires full build/native regression checks.
- 9be0c87 navigation diagnostics: first Run label reports layout size 40×28 versus paragraph width 148. Explicit full-width label layout now matches the tab's allocated width; overflow assertions remain mandatory. This is pending verification, not a waived assertion.

### Verified follow-up at e537c44

API 34 gallery `screen-13.png` visually inspected: all three permission descriptions/statuses, the app-settings action and the notification-only cleanup explanation fit the fixture viewport. Location now explicitly says precise access. This fixture grants permissions in advance; the image does not prove denied states or settings-return refresh. A dedicated permission suite now covers those transitions, awaiting execution.

API 35 large-font capture from 35932263677 inspected at `C:/Users/gana0/StepUp-captures/redesign-e537c44/api-35-large-font/screen-gallery/large-font-forms/inventory-filter-reset-1.6.png`: the full Reset and Show results controls are above the gesture navigation area. Its stricter bounds/interaction test passes. API 34 interaction (including all Chrome viewport scenarios), gallery and large-font jobs also passed. Other dialog states, current edits and physical devices are not covered by this observation.
