# Drive / Charge Detail Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add read-only slide-in detail overlays for individual drives and charges, opened by tapping a list item in `DrivingScreen` / `ChargingScreen`.

**Architecture:** Reuse existing `BatteryDetailScreen` pattern from `DashboardScreen.kt` — a composable rendered inside `AnimatedVisibility` with `slideInVertically` / `slideOutVertically`. Selection state (`selectedDriveId: Int?`, `selectedChargeId: Int?`) lives in existing ViewModels; detail composables are stateless and receive a DTO plus `onClose` callback. No new API calls — reuse already-fetched `DriveDto` / `ChargeDto` lists.

**Tech Stack:** Kotlin Multiplatform (commonMain), Jetpack Compose, Material3, `androidx.lifecycle` ViewModel + StateFlow.

**Note on testing:** The project currently has no test harness. This plan uses manual QA checkpoints (Android `./gradlew :composeApp:assembleDebug` + device smoke test) instead of TDD. Adding a test framework is out of scope.

---

## File Structure

**Created:**
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DriveDetailScreen.kt` — stateless detail composable for one drive
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargeDetailScreen.kt` — stateless detail composable for one charge

**Modified:**
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingViewModel.kt` — add `selectedDriveId` + `selectedDrive` flows
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingViewModel.kt` — add `selectedChargeId` + `selectedCharge` flows
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingScreen.kt` — wrap list in Box, pass onTap to `DriveItem`, host overlay
- `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingScreen.kt` — wrap list in Box, pass onTap to `ChargeItem`, host overlay

---

## Task 1: DrivingViewModel — add selection flows

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingViewModel.kt`

- [ ] **Step 1: Add selection state + combined flow**

Replace the body of `DrivingViewModel` (keep package + existing imports) so the final file looks exactly like this:

```kotlin
package com.soooool.matedash.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.DriveDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrivingViewModel : ViewModel() {
    private val _drives = MutableStateFlow<List<DriveDto>>(emptyList())
    val drives: StateFlow<List<DriveDto>> = _drives.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDriveId = MutableStateFlow<Int?>(null)
    val selectedDriveId: StateFlow<Int?> = _selectedDriveId.asStateFlow()

    val selectedDrive: StateFlow<DriveDto?> =
        combine(_drives, _selectedDriveId) { list, id ->
            if (id == null) null else list.firstOrNull { it.driveId == id }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var loaded = false

    fun loadDrivesIfNeeded() {
        if (!loaded) {
            loaded = true
            loadDrives()
        }
    }

    fun loadDrives() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _drives.value = ServiceLocator.apiClient.getDrives(config)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "주행 기록 로드 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDrive(id: Int?) {
        _selectedDriveId.value = id
    }

    fun clearSelection() {
        _selectedDriveId.value = null
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingViewModel.kt
git commit -m "feat(driving): add selectedDrive state to ViewModel"
```

---

## Task 2: ChargingViewModel — add selection flows

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingViewModel.kt`

- [ ] **Step 1: Add imports**

In the import block of `ChargingViewModel.kt`, add these three lines next to the other `kotlinx.coroutines.flow.*` imports (they are added, not replacing):

```kotlin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
```

- [ ] **Step 2: Add selection fields + flow + setters inside the class**

Insert this block inside `ChargingViewModel` right after the existing `_errorMessage` / `errorMessage` declarations (around the line `private var loaded = false`):

```kotlin
    private val _selectedChargeId = MutableStateFlow<Int?>(null)
    val selectedChargeId: StateFlow<Int?> = _selectedChargeId.asStateFlow()

    val selectedCharge: StateFlow<ChargeDto?> =
        combine(_allCharges, _selectedChargeId) { list, id ->
            if (id == null) null else list.firstOrNull { it.chargeId == id }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectCharge(id: Int?) {
        _selectedChargeId.value = id
    }

    fun clearSelection() {
        _selectedChargeId.value = null
    }
```

- [ ] **Step 3: Build to verify**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingViewModel.kt
git commit -m "feat(charging): add selectedCharge state to ViewModel"
```

---

## Task 3: Create `DriveDetailScreen.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DriveDetailScreen.kt`

- [ ] **Step 1: Write the full file**

Create `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DriveDetailScreen.kt` with exactly this content:

```kotlin
package com.soooool.matedash.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.data.api.DriveDto
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val TeslaRed = Color(0xFFE31937)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)

private fun Int.withComma(): String {
    val neg = this < 0
    val s = (if (neg) -this else this).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return if (neg) "-$sb" else sb.toString()
}

private fun Double.fmt1(): String {
    val rounded = (this * 10).roundToInt()
    return "${(rounded / 10).withComma()}.${abs(rounded % 10)}"
}

private fun Double.fmt0() = roundToInt().withComma()

private fun formatDateTime(dateStr: String?): String {
    if (dateStr == null) return "-"
    return try {
        val t = dateStr.indexOf('T')
        if (t < 0) return dateStr
        val datePart = dateStr.substring(0, t)
        val timePart = dateStr.substring(t + 1, minOf(t + 6, dateStr.length))
        "$datePart $timePart"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun DriveDetailScreen(drive: DriveDto, onClose: () -> Unit) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val distance = drive.odometerDetails?.odometerDistance
    val energy = drive.energyConsumedNet
    val efficiencyText: String = if (distance != null && distance > 0 && energy != null) {
        "${((energy * 1000) / distance).fmt0()} Wh/km"
    } else "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = statusBarTop + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CardBg,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "주행 상세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${drive.startAddress ?: "출발지"} → ${drive.endAddress ?: "도착지"}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                    )
                }
            }
        }

        // 주행 요약
        item {
            DetailCard("주행 요약", accent = TeslaRed) {
                DetailRow("거리", distance?.let { "${it.fmt1()} km" } ?: "-")
                DetailRow("소요시간", drive.durationStr ?: drive.durationMin?.let { "${it}분" } ?: "-")
                DetailRow("출발", formatDateTime(drive.startDate))
                DetailRow("도착", formatDateTime(drive.endDate))
            }
        }

        // 배터리
        item {
            DetailCard("배터리", accent = BatteryGreen) {
                val startBat = drive.batteryDetails?.startBatteryLevel
                val endBat = drive.batteryDetails?.endBatteryLevel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BatteryPill(label = "시작", percent = startBat)
                    Text("→", fontSize = 20.sp, color = TextSecondary)
                    BatteryPill(label = "종료", percent = endBat)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                DetailRow("사용 에너지", energy?.let { "${it.fmt1()} kWh" } ?: "-")
                DetailRow("효율", efficiencyText)
            }
        }

        // 속도 / 환경
        item {
            DetailCard("속도 / 환경", accent = ChargingBlue) {
                DetailRow("평균 속도", drive.speedAvg?.let { "${it.fmt0()} km/h" } ?: "-")
                DetailRow("최고 속도", drive.speedMax?.let { "${it.withComma()} km/h" } ?: "-")
                DetailRow("외기온 평균", drive.outsideTempAvg?.let { "${it.fmt1()} °C" } ?: "-")
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DetailCard(title: String, accent: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun BatteryPill(label: String, percent: Int?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = percent?.let { "$it%" } ?: "-",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}
```

- [ ] **Step 2: Build to verify**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DriveDetailScreen.kt
git commit -m "feat(driving): add DriveDetailScreen composable"
```

---

## Task 4: Wire `DrivingScreen` to open detail overlay

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingScreen.kt`

- [ ] **Step 1: Add imports**

Add these imports to the top of `DrivingScreen.kt` (next to existing imports):

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
```

- [ ] **Step 2: Add `onClick` to `DriveItem` signature**

Change the `DriveItem` composable declaration and add a `clickable` modifier.

Find:
```kotlin
@Composable
private fun DriveItem(drive: DriveDto) {
```
Replace with:
```kotlin
@Composable
private fun DriveItem(drive: DriveDto, onClick: () -> Unit) {
```

Then in the first `Column(` inside `DriveItem`, change:
```kotlin
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
```
to:
```kotlin
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
```

- [ ] **Step 3: Wrap `LazyColumn` in a `Box` and add overlay**

Find the current body of `DrivingScreen` starting with `LazyColumn(` and its matching closing brace (around lines 96–158). Wrap that `LazyColumn` in a `Box(modifier = Modifier.fillMaxSize())`, and after it (before the outer `Box` closing brace) add an `AnimatedVisibility` block.

The relevant function body after the change should look like:

```kotlin
@Composable
fun DrivingScreen() {
    val vm = viewModel { DrivingViewModel() }
    val drives by vm.drives.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()
    val selectedDrive by vm.selectedDrive.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadDrivesIfNeeded()
        if (drives.isEmpty() && !isLoading && errorMsg != null) {
            vm.loadDrives()
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = statusBarTop + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "주행 기록",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = TeslaRed, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (errorMsg != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "⚠️ $errorMsg",
                            color = TeslaRed,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "다시 시도",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChargingBlue,
                            modifier = Modifier
                                .clickable { vm.loadDrives() }
                                .padding(8.dp),
                        )
                    }
                }
            } else {
                item {
                    DriveSummaryCard(drives)
                }

                items(drives) { drive ->
                    DriveItem(drive = drive, onClick = { vm.selectDrive(drive.driveId) })
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        AnimatedVisibility(
            visible = selectedDrive != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250),
            ) + fadeOut(animationSpec = tween(250)),
        ) {
            selectedDrive?.let { d ->
                DriveDetailScreen(drive = d, onClose = { vm.clearSelection() })
            }
        }
    }
}
```

- [ ] **Step 4: Build to verify**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/driving/DrivingScreen.kt
git commit -m "feat(driving): open DriveDetailScreen on list item tap"
```

---

## Task 5: Create `ChargeDetailScreen.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargeDetailScreen.kt`

- [ ] **Step 1: Write the full file**

Create `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargeDetailScreen.kt` with exactly this content:

```kotlin
package com.soooool.matedash.ui.charging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.data.api.ChargeDto
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val CostGold = Color(0xFFFFD60A)

private fun Int.withComma(): String {
    val neg = this < 0
    val s = (if (neg) -this else this).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return if (neg) "-$sb" else sb.toString()
}

private fun Double.fmt1(): String {
    val rounded = (this * 10).roundToInt()
    return "${(rounded / 10).withComma()}.${abs(rounded % 10)}"
}

private fun Double.fmt0() = roundToInt().withComma()

private fun formatDateTime(dateStr: String?): String {
    if (dateStr == null) return "-"
    return try {
        val t = dateStr.indexOf('T')
        if (t < 0) return dateStr
        val datePart = dateStr.substring(0, t)
        val timePart = dateStr.substring(t + 1, minOf(t + 6, dateStr.length))
        "$datePart $timePart"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun ChargeDetailScreen(charge: ChargeDto, onClose: () -> Unit) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val energyAdded = charge.chargeEnergyAdded
    val durationMin = charge.durationMin
    val avgPowerText: String = if (energyAdded != null && durationMin != null && durationMin > 0) {
        "${((energyAdded * 60.0) / durationMin).fmt1()} kW"
    } else "-"

    val cost = charge.cost
    val perKwhText: String = if (cost != null && energyAdded != null && energyAdded > 0) {
        "${(cost / energyAdded).fmt0()} 원/kWh"
    } else "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = statusBarTop + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CardBg,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "충전 상세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = charge.address ?: "위치 없음",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                    )
                }
            }
        }

        // 충전 요약
        item {
            DetailCard("충전 요약", accent = ChargingBlue) {
                DetailRow("소요시간", charge.durationStr ?: charge.durationMin?.let { "${it}분" } ?: "-")
                DetailRow("추가 에너지", energyAdded?.let { "${it.fmt1()} kWh" } ?: "-")
                DetailRow("평균 충전 출력", avgPowerText)
                if (charge.chargeEnergyUsed != null) {
                    DetailRow("사용 에너지", "${charge.chargeEnergyUsed.fmt1()} kWh")
                }
                DetailRow("시작", formatDateTime(charge.startDate))
                DetailRow("종료", formatDateTime(charge.endDate))
            }
        }

        // 배터리
        item {
            DetailCard("배터리", accent = BatteryGreen) {
                val startBat = charge.batteryDetails?.startBatteryLevel
                val endBat = charge.batteryDetails?.endBatteryLevel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BatteryPill(label = "시작", percent = startBat)
                    Text("→", fontSize = 20.sp, color = TextSecondary)
                    BatteryPill(label = "종료", percent = endBat)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                val diff = if (startBat != null && endBat != null) "+${endBat - startBat}%p" else "-"
                DetailRow("증가", diff)
            }
        }

        // 비용 / 환경
        item {
            DetailCard("비용 / 환경", accent = CostGold) {
                DetailRow("비용", cost?.let { "${it.fmt0()} 원" } ?: "-")
                DetailRow("kWh 당", perKwhText)
                DetailRow("외기온 평균", charge.outsideTempAvg?.let { "${it.fmt1()} °C" } ?: "-")
                DetailRow("주행계", charge.odometer?.let { "${it.fmt0()} km" } ?: "-")
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DetailCard(title: String, accent: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun BatteryPill(label: String, percent: Int?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = percent?.let { "$it%" } ?: "-",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}
```

- [ ] **Step 2: Build to verify**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargeDetailScreen.kt
git commit -m "feat(charging): add ChargeDetailScreen composable"
```

---

## Task 6: Wire `ChargingScreen` to open detail overlay

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingScreen.kt`

- [ ] **Step 1: Add imports**

Add these imports to the top of `ChargingScreen.kt`:

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
```

- [ ] **Step 2: Add `onClick` to `ChargeItem` signature**

Find:
```kotlin
private fun ChargeItem(charge: ChargeDto) {
```
Replace with:
```kotlin
private fun ChargeItem(charge: ChargeDto, onClick: () -> Unit) {
```

In the same function, find the root `Column(` modifier chain and insert `.clickable { onClick() }` between `.background(...)` and `.padding(...)` (or `.fillMaxWidth()` chain — place it immediately before the final `.padding(...)` call on the outermost `Column`'s modifier). Example:

```kotlin
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
```

- [ ] **Step 3: Update call site and wrap with overlay**

In `ChargingScreen` composable body find:

```kotlin
            items(filteredCharges) { charge -> ChargeItem(charge) }
```

Replace with:

```kotlin
            items(filteredCharges) { charge ->
                ChargeItem(charge = charge, onClick = { vm.selectCharge(charge.chargeId) })
            }
```

Collect selected charge state near the top of `ChargingScreen` (after the other `collectAsState` lines):

```kotlin
    val selectedCharge by vm.selectedCharge.collectAsState()
```

Locate the outermost layout in `ChargingScreen` (it is a `Column` that contains the MonthYearSelector + `LazyColumn`). Wrap that outermost layout in `Box(modifier = Modifier.fillMaxSize())`, then add the `AnimatedVisibility` block **inside** the Box, after the existing content.

The overlay block to add:

```kotlin
        AnimatedVisibility(
            visible = selectedCharge != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250),
            ) + fadeOut(animationSpec = tween(250)),
        ) {
            selectedCharge?.let { c ->
                ChargeDetailScreen(charge = c, onClose = { vm.clearSelection() })
            }
        }
```

If the existing `ChargingScreen` root is already a `Column` (not a `Box`), switch it to a `Box` containing a `Column` (for the existing UI) and the `AnimatedVisibility` block side-by-side. The resulting skeleton:

```kotlin
@Composable
fun ChargingScreen() {
    val vm = viewModel { ChargingViewModel() }
    // ... existing collectAsState lines ...
    val selectedCharge by vm.selectedCharge.collectAsState()

    // ... existing LaunchedEffect / setup ...

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
            // ... entire existing UI block (MonthYearSelector + LazyColumn + etc.) ...
        }

        AnimatedVisibility(
            visible = selectedCharge != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250),
            ) + fadeOut(animationSpec = tween(250)),
        ) {
            selectedCharge?.let { c ->
                ChargeDetailScreen(charge = c, onClose = { vm.clearSelection() })
            }
        }
    }
}
```

> If the current root is already a `Box`, just add the `AnimatedVisibility` block as a new child and skip the wrapping.

- [ ] **Step 4: Build to verify**

Run:
```
./gradlew :composeApp:compileCommonMainKotlinMetadata
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add composeApp/src/commonMain/kotlin/com/soooool/matedash/ui/charging/ChargingScreen.kt
git commit -m "feat(charging): open ChargeDetailScreen on list item tap"
```

---

## Task 7: Full Android build + manual QA

**Files:** none (verification only)

- [ ] **Step 1: Full Android debug build**

Run:
```
./gradlew :composeApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Any Kotlin or resource error here must be fixed in the relevant prior task — do not paper over with try/catch.

- [ ] **Step 2: Manual QA checklist (Android on device/emulator)**

Install the APK and verify each of:

- [ ] Open **주행** tab → tap any drive row → detail slide-in appears from bottom with `주행 상세` title, correct from→to, 3 cards populated
- [ ] Tap close (X) → detail slides out, back to list unchanged
- [ ] Scroll the drive detail → no layout glitches at top (status bar) or bottom
- [ ] Find a drive with null `energyConsumedNet` (older records) → `사용 에너지` and `효율` show `-` (not `0` or a crash)
- [ ] Open **충전** tab → tap any charge row → detail slide-in appears with `충전 상세` title, correct address, 3 cards populated
- [ ] Verify `평균 충전 출력` matches rough expectation: `추가 에너지(kWh) × 60 / 소요시간(분)`
- [ ] Find a charge with null `cost` → `비용` and `kWh 당` show `-`
- [ ] Close charge detail → back to charge list, month selector still correct

- [ ] **Step 3: iOS build sanity (if Xcode available)**

Run:
```
./gradlew :composeApp:compileKotlinIosArm64
```
Expected: BUILD SUCCESSFUL (common code compiles for iOS). Full iOS UI test is optional for this MVP.

- [ ] **Step 4: Final commit if any fix was needed**

If any fix was required during QA:
```
git add -A
git commit -m "fix(drive-charge-detail): address QA findings"
```

Otherwise skip this step.

---

## Summary — what this plan delivers

- Two new read-only detail overlays opened by tapping list rows in Driving / Charging tabs
- Zero new API calls — uses already-fetched DTOs
- Consistent styling with existing dashboard detail pattern
- Safe handling of null fields (displays `-`, avoids divide-by-zero)
- Out of scope (future): route map, charge kW curve, cost editing, back-gesture dismiss
