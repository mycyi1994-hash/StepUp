# Board brief — StepUp (Android running app, Korea)

You are reviewing a business. Everything you know about it is on this page. Do not assume facts that are not here; where a fact is missing, treat it as an open question.

## The idea (from the founder's files)

StepUp is a free Android running app for Korean runners. Store name planned: "StepUp — 러닝 크루". Category: Health & Fitness. Languages: Korean (primary), English, Japanese, Chinese. Not yet published on Google Play; a test APK is distributed directly to testers. Built by one founder with AI coding tools.

### What the product does today (built and working in the test build)
- **Run tracking**: GPS route, distance, pace, laps, lock-screen live notification, result card sharing, courses you can record, save and share; weekly leaderboard (crew and individual distance).
- **Crews**: create a crew, invite links, crew chat, crew home, "flash run" meetups, and **crew runs**: members in one room start together; the server tracks one shared "crew stamina" pool per crew per day (built from the max durability of the shoes each runner picked, boosted by crew level). Each runner uses 5 stamina per minute; when it hits 0, rewards stop but the run keeps recording. One crew run per person per day. A headcount bonus (+5% for 2 people … +50% at 6 … +150% at 20) applies only to people from the same crew who stayed within 500 m of each other at the start and 2 minutes in, and who have verified a phone number (no one has phone verification yet, so this bonus is effectively 0 today). Crew levels 1–20 grow from members' run distance.
- **Game layer**: running earns "SUP" points (about 0.01 per step during a run, multiplied by the equipped shoe's efficiency). Virtual sneakers in 6 rarities come from free draws; each shoe wears down 5 durability per minute of running and must be repaired with SUP; shoes level up with SUP; shoes can be traded between users for SUP. There is anti-cheat (speed limit stop with penalty, GPS checks, daily caps).
- **AI coaching** screens exist but the AI is switched off.
- **Blockchain**: an ERC-20 token and sneaker NFTs exist on a test network (GIWA Sepolia), and wallet linking works in the test build. In the Google Play build all of this is hidden: Google Play requires a Korean FIU virtual-asset-business registration for crypto wallet/exchange apps from 2026-01-28. SUP in the store build is a points balance with no cash value.

### Money
- Price to the user: free. No ads, no subscription, no in-app purchases, no paid draws (all draws are free).
- **No revenue model has been decided.** Costs: one founder's time, Supabase backend (Singapore region), Cloudflare web hosting, map tiles (MapTiler/OpenStreetMap), Google Play developer account ($25). Budget, runway and team beyond the founder: not stated.

### Founder's own thesis (from the pitch docs)
- "Fitness apps churn; move-to-earn apps collapse when token emissions fall, because the token was the product. We built the app first and the token second — the app should be worth opening without the token."
- Earnings are capped by design: the best shoe earns only modestly more than a free one ("capital cannot outrun legs").
- Plan: launch v1 off-chain (points only, no withdrawal) to prove people actually use it, then decide on on-chain later.

### Open questions nobody has answered yet
- Target customer size in Korea, cost to acquire a user, how the first 100 users arrive.
- How the business will ever earn money.
- Legal review of points, shoe trading between users, wallet, withdrawal; location-service business filing.
- No real user metrics yet (designed but not collected for evidence).
