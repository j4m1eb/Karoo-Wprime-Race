# W′ Race — Karoo Extension

A [Hammerhead Karoo](https://www.hammerhead.io) extension that tracks your **W′ balance** (anaerobic capacity) in real time and compares it against a target pacing curve for time trials and criteriums.

---

## What Is W′?

W′ (pronounced *W-prime*) is the finite amount of work you can perform above your **Critical Power (CP)**. Every second you ride above CP, W′ depletes. Every second below CP, it recovers. When W′ reaches zero you're done — the tank is empty.

Knowing your W′ balance isn't just interesting — it tells you exactly how hard you can go and for how long. This extension takes that number and compares it to where you *should* be at this point in the race.

---

## Data Fields

### W′ TT — Time Trial Pacing
Compares your current W′ balance against a **linear target curve** for your chosen TT duration.

The target starts at 100 % and depletes linearly to 0 % at the finish line. If you're ahead of the curve you can push harder; if you're behind, ease up to survive.

**Field layout (single-width):**
```
T: 97%       ← target for this moment
↘  89%       ← your current W′ balance + trend arrow
```

**Field layout (double-width):**
```
         W′ BALANCE
T: 97%     ↘    89%
```

---

### W′ Crit — Criterium Pacing
Compares your W′ balance against the **floor for the current race phase**. The target is a step function — it holds constant across the phase and drops at each phase boundary.

**T: shows the floor you must not go below right now.** When the phase changes, T: jumps down to the next floor. This is intentionally different from the TT field — in a criterium you cannot and should not try to pace W′ per-second; you manage a floor.

Layout identical to W′ TT above.

---

### W′ Usable
Shows how much W′ is available **above your current phase floor** — the amount you can actually spend on attacks and accelerations without compromising your race plan.

A high number means headroom to attack. Near zero means you're already at your limit for this phase.

Displays as **%** of total W′ (default) or **kJ** (configurable).

---

### W′ Time to Floor
Shows how many **seconds** you can sustain your current 3s power before W′ drops to the **current phase floor**. Displays `---` when you're recovering (power ≤ CP).

Use this to judge how long you can stay on a wheel or cover an attack before you hit your phase limit.

---

### W′ Time to Empty
Shows how many **seconds** you can sustain your current 3s power before W′ hits **0% — going pop**. Displays `---` when recovering.

No phase curve — this is the absolute limit regardless of race phase. Combined with Time to Floor it gives you two views: "how long until I breach my plan" and "how long until I completely blow up".

---

## Colour Coding

### W′ Crit, TT (gap between actual and target)

| Colour | Meaning |
|--------|---------|
| 🟢 **Green** | On pace or ahead — you can safely push |
| 🟠 **Orange** | Slightly behind (gap ≤ 8 %) — manage your effort |
| 🔴 **Red** | Well behind (gap 8–20 %) — back off |
| 🟣 **Purple** | Critical (gap > 20 %) — in danger of blowing up |

### W′ Usable (headroom above floor)

| Colour | Meaning |
|--------|---------|
| 🟢 **Green** | > 20 % headroom — comfortable |
| 🟠 **Orange** | 10–20 % — shrinking |
| 🔴 **Red** | 5–10 % — nearly at floor |
| 🟣 **Purple** | < 5 % — at or below floor |

### W′ Time to Floor / Time to Empty

| Colour | Meaning |
|--------|---------|
| 🟢 **Green** | > 20 s |
| 🟠 **Orange** | 10–20 s |
| 🔴 **Red** | 5–10 s |
| 🟣 **Purple** | < 5 s |

---

## Trend Arrow

An optional animated arrow shows the direction your W′ is moving:

- **Arrow pointing up-left** — recovering strongly (power well below CP)
- **Arrow pointing right** — neutral (riding at CP)
- **Arrow pointing down-right** — depleting (power above CP)

The arrow rotates through 13 positions (−90° to +90°) proportional to how far above or below CP you're riding.

---

## W′ Models

Four scientifically validated models are available. All use the same CP and W′ parameters, but differ in how recovery is calculated:

| Model | Year | Notes |
|-------|------|-------|
| **Skiba Differential** | 2014 | Default. Fast, continuous recovery based on W′ deficit |
| **Skiba 2012** | 2012 | Simpler original Skiba formulation |
| **Bartram** | 2018 | Recovery rate scales with power below CP |
| **Chorley (Bi-Exp)** | 2023 | Dual-exponential recovery — fast and slow components |

If unsure, start with **Skiba Differential**. Chorley is the most physiologically realistic but requires more testing against your own data.

---

## Setup

### 1. Install
Download the latest APK from [Releases](https://github.com/j4m1eb/Karoo-Wprime-Race/releases) and sideload it onto your Karoo (Settings → Developer Options → Install APK).

### 2. Configure
Open the **W Prime Race** app on your Karoo and set:

| Setting | Description |
|---------|-------------|
| **TT Critical Power (W)** | CP/pacing baseline used only by the W′ TT field |
| **TT W′ Budget (kJ)** | W′ budget used only by the W′ TT field |
| **TT Duration (min)** | Your target finish time for time trial events |
| **Crit Critical Power (W)** | CP used by the crit fields |
| **Crit Anaerobic Capacity (kJ)** | W′ used by the crit fields |
| **Crit Duration (min)** | Total expected race duration for criteriums |
| **W′ Model** | Algorithm used for W′ tracking (see above) |
| **Show Trend Arrow** | Show/hide the rotating direction arrow |
| **TT / Crit / Usable — show kJ** | Display values in kJ instead of % |

Tap **Save** — the button turns green to confirm.

TT and Crit parameters are saved separately. This lets you use a TT pacing-budget setup, for example CP just below target power with a fixed kJ overage budget, without disturbing your physiological crit CP/W′ settings.

#### Crit Pacing Curve

Below the crit duration is a **Pacing Curve** editor. This defines 4 phase breakpoints — the W′ floor to hold during each phase of the race.

| Column | Description |
|--------|-------------|
| **Race %** | The point at which this phase ends |
| **W′ floor %** | Minimum W′ to hold during this phase |

The minute equivalent of each Race % updates dynamically based on the crit duration you've set above.

The target is a **step function** — it holds at the phase floor and jumps down at each boundary. This is intentional: "don't go below 70% for the first 43% of the race" is actionable; a constantly moving target is not.

The start (0 % → 100 % W′) and finish (100 % → 0 %) are always fixed. Only the 4 interior points are editable.

Tap **Reset to Default** to restore the recommended strategy:

| Race % | ≈ Time (65 min) | W′ floor | Phase |
|--------|-----------------|----------|-------|
| 43 % | 28 min | 70 % | Opening — conserve, cover wheels |
| 71 % | 46 min | 50 % | Mid-race — controlled aggression |
| 97 % | 63 min | 30 % | Build — commit to moves |
| 99 % | 64 min | 10 % | Finale — commit to the sprint |

After the last breakpoint, the target drops to 0 % — empty the tank.

### 3. Add Fields
On your Karoo, go to a ride profile → Add fields → scroll to **W Prime Race** and add any combination of the five fields to your data pages.

Fields support single-width and double-width layouts.

---

## Finding Your Numbers

- **Critical Power**: Use a structured ramp test, 20-minute FTP test (×0.95), or 3-minute all-out test. Most training platforms (Intervals.icu, TrainingPeaks, Garmin) can estimate CP.
- **W′**: Often reported alongside CP. Typical values are 10–25 kJ for trained cyclists. If unknown, start with 20 kJ and refine from training data.

---

## How Pacing Curves Work

### Time Trial (linear)
```
Target W′% = 100 × (1 − elapsed / duration)
```
At the halfway point your target is 50 %. A perfectly paced TT empties W′ at exactly the finish line.

The TT field uses the TT CP/W′/duration block only.

### Criterium (step function, customisable)
The crit target is a **step function** based on phase floors. The target holds constant across each phase and jumps down at the boundary. The default strategy:

| Phase | Race elapsed | W′ floor |
|-------|-------------|----------|
| Opening | 0–43 % | 70 % — conserve, cover wheels |
| Mid-race | 43–71 % | 50 % — controlled aggression |
| Build | 71–97 % | 30 % — commit to moves |
| Finale | 97–100 % | 0 % — empty the tank |

All crit fields (W′ Crit, Usable, Time to Floor) use the same phase boundaries — they are fully consistent with each other.

The crit fields use the Crit CP/W′/duration/curve block only.

---

## Technical Notes

- W′ updates from the **3s smoothed power** stream — responsive but not jerky
- TT and Crit maintain separate CP/W′ settings, so race-type changes do not require re-entering numbers
- The extension resets W′ at the start of each new recording
- Elapsed time is anchored to when recording starts — warm-up before a race does not corrupt the phase calculation
- Settings update live — changes take effect within one refresh cycle
- Fields work in both **portrait** (standard) and **landscape** layouts
- Compatible with Karoo 2 and Karoo 3

---

## Requirements

- Hammerhead Karoo 2 or Karoo 3
- Karoo software version 1.524+
- A power meter

---

## Credits

Built using the [Karoo Extensions SDK](https://github.com/hammerheadnav/karoo-ext) by Hammerhead.

W′ modelling based on published research by Skiba, Bartram, Chorley, and colleagues.

---

## Licence

MIT — free to use, modify, and share.
