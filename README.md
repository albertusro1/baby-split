<p align="center">
  <img src="app/src/main/res/drawable/ic_baby_split_logo.jpg" width="140" height="140" style="border-radius: 28px;" alt="Baby Split Logo" />
</p>

<h1 align="center">🐥 Baby Split</h1>
<h3 align="center">Real-Time Collaborative Bill Splitting & Trip Expense Manager for Android with 1-Tap WhatsApp Sharing</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-brightgreen.svg" alt="Android Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.x-blue.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-purple.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Backend-Firebase_Firestore_%26_Auth-orange.svg" alt="Firebase Backend" />
  <img src="https://img.shields.io/badge/Database-Room_SQLite_(Offline_Default)-yellow.svg" alt="Room Database" />
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License" />
</p>

---

## 📖 About Baby Split

**Baby Split** is a modern native Android application built with Jetpack Compose designed to effortlessly split bills, track trip expenses, simplify circular debts, and share itemized breakdowns directly to friends via **WhatsApp**.

Baby Split combines the best of two worlds:
1. **📴 100% Offline-First by Default**: Works immediately on-device with Room SQLite without requiring an account or internet connection.
2. **☁️ Live Real-Time Collaboration (Firebase)**: Sign in with Google to enable automatic cloud backup and live multi-user editing. Invite friends to any trip using a simple **8-character invite code**—any group member can add, edit, or delete expenses and see updates reflected in real time!

---

## 🌟 Key Features

### ☁️ 1. Real-Time Cloud Sync & Multi-User Collaboration
- **Google Sign-In**: Native authentication using the latest AndroidX Credential Manager API.
- **8-Character Trip Codes**: Share an invite code or link to let trip members join.
- **Live Collaborative Editing**: When any group member adds an expense or records a settlement, changes sync instantly across all devices.
- **Zero Server Costs**: Powered by Google Cloud Firestore on the free Spark tier.

---

### 📴 2. Offline-First Default Mode
- Don't want to sign in? The app functions 100% offline out-of-the-box using local **Room SQLite**.
- Full access to all 5 split algorithms, debt simplification, receipt photos, and WhatsApp sharing with zero internet needed.

---

### 💬 3. 1-Tap WhatsApp Sharing
Generate and send formatted WhatsApp summaries with one tap:

#### 🔹 Individual Member Breakdown
```text
🧾 *Baby Split - Bali Vacation 2026*
📅 Date: Aug 20, 2026

Hi *Alice*! Here is your itemized expense breakdown:

1. 🍔 *Dinner at Trattoria*
   • Total: $100.00
   • Your Share: *$25.00*

2. 🏨 *Villa Rental (3 Nights)*
   • Total: $300.00
   • Your Share: *$60.00*

3. 🍹 *Beach Club Drinks*
   • Total: $46.50
   • Your Share: *$15.50*

----------------------------------------
💰 *TOTAL AMOUNT YOU OWE: $100.50*
----------------------------------------

💳 *Please transfer to:*
• *Bank BCA*: 123-456-789 (a.n. Rowan)
• *PayPal*: paypal.me/rowan

Thank you! Generated with Baby Split 👶
```

#### 🔹 Group Settlement Summary
```text
📊 *Baby Split - Bali Vacation 2026 (Group Summary)*
📅 Date: Aug 20, 2026
💵 Total Group Spending: *$446.50*

🤝 *Settlement Breakdown (Simplified Debts):*
----------------------------------------
• *Alice* pays *Rowan*: $100.50
• *Bob* pays *Rowan*: $85.00
• *Charlie*: Settled up ✅
----------------------------------------

💳 *Host Payment Info:*
• Bank BCA: 123-456-789 (Rowan)
```

---

### 💸 4. Redesigned "Balances & Settle" Experience
- **Hero Settlement Status Banner**: Shows outstanding group debt and count of remaining payments at a glance.
- **⚡ Simplified Repayments ($O(N-1)$ algorithm)**: Greedy bipartite matching minimizes circular debts into the fewest direct transfers.
- **👥 Member Balances & Bank Info**: Clean cards showing who gets back or owes money, with tap-to-edit bank account / QRIS details.
- **Quick Record Settlement**: 1-tap settlement modal to clear balances as transfers occur.

---

### ⚡ 5. 5-Way Mathematical Splitting Engine
High-precision integer-cent math (`SplitCalculator.kt`) with deterministic remainder distribution guaranteeing zero-sum errors ($\sum \text{Shares} == \text{TotalAmount}$):
- **`=` Equal Split**: Even distribution with deterministic cent remainder assignment.
- **`$` Exact Amounts**: Explicit custom monetary allocations.
- **`%` Percentages**: Percentage splits with automatic rounding error correction.
- **`1/x` Shares / Ratios**: Weighted distribution by share counts.
- **`+/-` Adjustments**: Baseline equal split with custom plus/minus offsets.

---

## 🏗️ Architecture & Project Structure

```
com.babysplit.app
├── core/
│   ├── auth/           # AndroidX Credential Manager & Firebase Auth Repository
│   ├── firestore/      # Firestore Real-time Flow Listeners & Cloud Sync
│   ├── database/       # Room SQLite Database, DAOs, Entities, Local Repository
│   ├── repository/     # Unified TripRepository Domain Interface
│   ├── datastore/      # User Preferences & Host Payment Details
│   ├── whatsapp/       # WhatsApp Message Formatter & Direct Share Intents
│   ├── camera/         # Receipt Compression & Photo Handling
│   └── ui/theme/       # Material Design 3 Theme & Colors
├── feature/
│   ├── dashboard/      # Trips Dashboard, Create Trip & Join by Code Dialogs
│   ├── group/          # Trip Details (Expenses, Balances & Settle, Totals Tabs)
│   ├── expense/        # Add/Edit Expense, 5 Split Modes, Receipt Attachment
│   ├── balance/        # Debt Simplification Engine & Net Balance Calculations
│   ├── members/        # Invite Code Share Sheet & Member Management
│   ├── settlement/     # Record Settlement Dialog
│   └── profile/        # Google Account Management & Bank Transfer Info
└── navigation/         # Compose Navigation Routes & Screen Transitions
```

---

## 📦 How to Download and Build

### Method 1: Download from GitHub Actions (Recommended)
1. Navigate to the **[GitHub Actions](https://github.com/albertusro1/baby-split/actions)** tab.
2. Click the latest successful workflow run.
3. Download the **`BabySplit-Debug-APK`** artifact.
4. Install the `.apk` on your Android device!

### Method 2: Build Locally with Android Studio
1. Clone the repository:
   ```bash
   git clone https://github.com/albertusro1/baby-split.git
   ```
2. Open the project in **Android Studio** (Ladybug | 2024.2+ recommended).
3. Let Gradle sync.
4. Run `./gradlew assembleDebug` or click **Run** ▶️ to deploy to a connected device or emulator.

---

## 🧪 Testing

Run the automated unit test suite:
```bash
./gradlew test
```

Included unit tests:
- `SplitCalculatorTest`: Validates all 5 split modes and remainder cent distribution.
- `DebtSimplificationEngineTest`: Tests circular debt reduction and minimum transaction counts.
- `BillSummaryFormatterTest`: Tests WhatsApp message formatting and markdown generation.

---

## 📄 License

This project is licensed under the **MIT License**.
