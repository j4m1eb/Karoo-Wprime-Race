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
Compares your W′ balance against a **progressive depletion curve** designed for criteriums and mass-start races. The target depletes slowly at the start (conserve), faster mid-race, and targets near-zero at the finish (empty the tank at the right moment).

Layout identical to W′ TT above.

---

### W′ Usable
Shows how much W′ is available **above your current race floor** — the minimum you need to keep in reserve to stay on the pacing plan.

A high number means you have headroom to attack. Near zero means you're already at your limit.

Displays as **%** of total W′ (default) or **kJ** (configurable).

---

### W′ Time to Floor
Shows how many **seconds** you can sustain your current 3s power before W′ drops to the phase floor. Displays `---` when you're recovering (power ≤ CP).

Think of it as your remaining time-in-the-red budget.

---

## Colour Coding

All fields share the same colour scale based on how far W′ is behind the target:

| Colour | Meaning |
|--------|---------|
| 🟢 **Green** | On pace or ahead — you can safely push |
| 🟠 **Orange** | Slightly behind (gap ≤ 8 %) — manage your effort |
| 🔴 **Red** | Well behind (gap 8–20 %) — back off |
| 🟣 **Purple** | Critical (gap > 20 %) — in danger of blowing up |

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
| **Critical Power (W)** | Your CP — the cornerstone of all calculations |
| **Anaerobic Capacity (kJ)** | Your W′ in kilojoules (typically 10–30 kJ) |
| **TT Duration (min)** | Your target finish time for time trial events |
| **Crit Duration (min)** | Total expected race duration for criteriums |
| **W′ Model** | Algorithm used for W′ tracking (see above) |
| **Show Trend Arrow** | Show/hide the rotating direction arrow |
| **TT / Crit / Usable — show kJ** | Display values in kJ instead of % |

Tap **Save** — the button turns green to confirm.

#### Crit Pacing Curve

Below the crit duration is a **Pacing Curve** editor. This defines the 4 interior breakpoints of the depletion curve — the W′ floor you're targeting to hold at each stage of the race.

| Column | Description |
|--------|-------------|
| **Race %** | How far through the race this phase ends |
| **W′ floor %** | Minimum W′ balance to maintain until this point |

The minute equivalent of each Race % is shown dynamically beneath the field, updating automatically when you change the crit duration.

The start (0% → 100% W′) and finish (100% → 0% W′) are always fixed. Only the 4 interior points are editable.

Tap **Reset to Default** to restore the recommended strategy:

| Race % | ≈ Time (65 min) | W′ floor |
|--------|-----------------|----------|
| 43% | 28 min | 70% — conserve, cover wheels |
| 71% | 46 min | 50% — controlled aggression |
| 97% | 63 min | 30% — build to the finale |
| 99% | 64 min | 10% — commit to the sprint |

Between breakpoints the target is linearly interpolated. After the last breakpoint, the target drops to 0% at the finish — empty the tank.

### 3. Add Fields
On your Karoo, go to a ride profile → Add fields → scroll to **W Prime Race** and add any combination of the four fields to your data pages.

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

### Criterium (progressive, customisable)
The crit curve depletes in phases aligned with typical race dynamics. The exact breakpoints are fully configurable in the app settings. The default strategy:

| Phase | Race elapsed | W′ floor |
|-------|-------------|----------|
| Opening | 0–43 % | 70 % — conserve, cover wheels |
| Mid-race | 43–71 % | 50 % — controlled aggression |
| Build | 71–97 % | 30 % — commit to moves |
| Finale | 97–100 % | 0 % — empty the tank |

Between breakpoints the target is linearly interpolated. The colour coding reflects how close your W′ is to the floor for the current phase, not just the absolute value.

---

## Technical Notes

- W′ updates from the **3s smoothed power** stream — responsive but not jerky
- The extension resets W′ at the start of each new recording
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
