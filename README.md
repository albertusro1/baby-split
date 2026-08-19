# 👶 Baby Split

**Baby Split** is a modern, 100% on-device, standalone native Android application built with Kotlin and Jetpack Compose to manage shared expenses, split bills, simplify debts, and share itemized breakdowns directly via **WhatsApp** and **Gmail**.

Inspired by [`oss-apps/split-pro`](https://github.com/oss-apps/split-pro), Baby Split is completely decoupled from any external server infrastructure—offering zero server costs, maximum privacy, offline-first reliability, and seamless integration with the Google ecosystem (Google Drive & Gmail).

---

## 🌟 Key Features

- **📱 Single-Organizer (Host) Mode**: Only ONE person needs to install the app. Tag offline friends (name + optional phone) without requiring them to download anything.
- **✉️ Dual Member Types**:
  - **Offline Tagging**: Fast name-only tagging with optional WhatsApp phone number.
  - **Gmail Invitation**: Add members by Gmail address to receive automated itemized receipt summaries.
- **🏁 "Finish Trip" Trigger**: One tap to finalize the trip, automatically email receipts to Gmail-invited members, and back up the entire archive to Google Drive.
- **💬 1-Tap WhatsApp Sharing**:
  - **Individual Member Breakdown**: Itemized bill listing every expense they joined, personal share, total owed, and host payment details (Bank/E-wallet).
  - **Group Summary Matrix**: Simplified settlement table ready for group chats.
  - **Single Expense Split**: Quick share right after dinner/payment.
- **⚡ 5-Way Mathematical Splitting Engine**:
  - **Equal (`=`)**: Equal split with exact integer remainder cent distribution.
  - **Exact (`$`)**: Custom monetary amounts with real-time budget validation.
  - **Percentage (`%`)**: Split by percentage with rounding correction.
  - **Shares (`1/x`)**: Ratio/share-based splitting.
  - **Adjustment (`+/-`)**: Base equal split with custom adjustments.
- **🤝 Debt Simplification**: $O(N-1)$ bipartite greedy debt minimization algorithm reducing circular transactions.
- **☁️ Google Drive Cloud Storage**: Auto-syncs trip archives and receipt photos to your personal Google Drive folder (`"Baby Split"`).
- **📷 Photo Receipt Capture**: CameraX & PhotoPicker with client-side WebP compression (<400KB).
- **📴 100% Offline-First**: Powered by Room SQLite Database.

---

## 🏗️ Architecture & Tech Stack

```
com.babysplit.app
├── core/
│   ├── auth/           # Google Credential Manager (Native Sign-In)
│   ├── gdrive/         # Google Drive Cloud Storage & Backup Engine
│   ├── gmail/          # Automated Gmail Receipt Dispatcher
│   ├── whatsapp/       # WhatsApp Message Formatter & Direct Share Intents
│   ├── database/       # Room SQLite Database, DAOs, Entities, Converters
│   ├── datastore/      # User Preferences & Host Payment Details
│   ├── camera/         # Receipt Compression & Photo Handling
│   └── ui/theme/       # Material Design 3 (Dynamic Colors, Dark/Light mode)
├── feature/
│   ├── dashboard/      # Dashboard, Trips List, Net Balances
│   ├── group/          # Trip Management (Expenses, Balances, Totals tabs, Finish Trip)
│   ├── members/        # Offline Tagging & Gmail Invite Dialog
│   ├── expense/        # Add/Edit Expense, Split Engine, Category & Receipt Picker
│   ├── balance/        # Debt Simplification Engine & Net Balances
│   ├── settlement/     # Record Settlement Dialog
│   └── profile/        # Host Profile, Bank/Payment Info, Google Sync Settings
└── navigation/         # Compose Navigation Routes & NavGraph
```

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/albertusro1/baby-split.git
   ```
2. Open the project in **Android Studio** (Ladybug / Meerkat or newer).
3. Let Gradle sync and build the project.
4. Run on an Android device or emulator running Android 8.0+ (API 26+).

---

## 🧪 Unit Tests

Run the test suite:
```bash
./gradlew test
```
Tests include:
- `SplitCalculatorTest`: Validates Equal, Exact, Percentage, Share, and Adjustment split engines.
- `DebtSimplificationEngineTest`: Validates circular debt reduction algorithms.
- `BillSummaryFormatterTest`: Validates WhatsApp markdown and HTML receipt generation.

---

## 📄 License

This project is licensed under the MIT License.
