# Drive / Charge Detail Pages — Design

_Date: 2026-04-24_

## Goal

Tesla Mate 웹 UI 의 drive / charge 상세 페이지와 유사한 역할을 하는 읽기 전용 상세 화면을 MateDash 에 추가한다. 사용자가 `DrivingScreen` / `ChargingScreen` 리스트의 항목을 탭하면 슬라이드인 오버레이로 해당 주행/충전의 세부 정보를 확인할 수 있어야 한다.

## Non-Goals (MVP)

- **지도 / 경로 polyline** — TeslaMate REST API v1 은 positions 데이터를 노출하지 않음. 향후 proxy 서버 또는 커스텀 엔드포인트가 준비되면 별도 iteration 으로 추가.
- **편집 기능** (비용 수정 등) — Tesla Mate 는 비용 편집 UI 를 제공하지만, MateDash 는 일단 읽기 전용.
- **삭제 / 아카이브** — 동일 이유로 제외.
- **충전 kW 곡선** — 시계열 데이터 없음.
- **별도 네비게이션 그래프** — 기존 `HorizontalPager` 기반 탭 구조 유지.

## Architecture

### UI Pattern: 슬라이드인 오버레이

기존 `DashboardScreen` 의 `BatteryDetailScreen` 패턴 재사용:

- 리스트 화면에 `selectedDriveId: Int?` / `selectedChargeId: Int?` state 보유
- non-null 이면 오버레이 composable 을 리스트 위에 렌더
- `AnimatedVisibility` + `slideIn/slideOut` horizontal 300ms
- Android 뒤로가기 / 상단 닫기 버튼으로 dismiss

### 데이터 흐름

- 추가 API 호출 없음. 이미 `DrivingViewModel.drives` / `ChargingViewModel.charges` 가 보유 중인 `DriveDto` / `ChargeDto` 목록에서 id 로 조회.
- `DriveDetailScreen(drive: DriveDto, onClose: () -> Unit)` / `ChargeDetailScreen(charge: ChargeDto, onClose: () -> Unit)` — stateless, DTO 만 받음.

### 파일 배치

```
commonMain/kotlin/com/soooool/matedash/ui/
├── driving/
│   ├── DrivingScreen.kt            (기존, 오버레이 호스트 + selectedDriveId 추가)
│   ├── DrivingViewModel.kt         (기존, 변경 없음)
│   └── DriveDetailScreen.kt        (신규)
└── charging/
    ├── ChargingScreen.kt           (기존, 오버레이 호스트 + selectedChargeId 추가)
    ├── ChargingViewModel.kt        (기존, 변경 없음)
    └── ChargeDetailScreen.kt       (신규)
```

### 재사용 컴포넌트

- `DetailCard(title, content)` — `DashboardScreen.kt` 에서 사용 중인 패턴 동일 스타일
- `DetailRow(label, value)` — 라벨 좌 / 값 우 행
- `BatteryGaugeCompact` — 시작 → 종료 % 표시
- `StatChip` — 주요 숫자 강조

필요한 경우 새 보조 composable 을 상세 파일 내부에 `private` 로 정의.

## Drive Detail Screen — Contents

### Header

- 제목: `출발주소 → 도착주소` (긴 주소는 줄임)
- 부제: 시작 일시 (`2026-04-24 07:29`)
- 우측 상단: 닫기(X) 아이콘 버튼

### Card 1: 주행 요약

| 필드 | 값 | 출처 |
|---|---|---|
| 거리 | `odometerDistance` km | `DriveDto.odometerDetails.odometerDistance` |
| 소요시간 | `duration_str` | `DriveDto.durationStr` |
| 시작 시각 | 포맷된 `start_date` | `DriveDto.startDate` |
| 종료 시각 | 포맷된 `end_date` | `DriveDto.endDate` |

### Card 2: 배터리

- 좌: `startBatteryLevel` % 원형 게이지 · 가운데: `→` (방향 표시 Text) · 우: `endBatteryLevel` % 원형 게이지
- `사용 에너지`: `energyConsumedNet` kWh
- `효율`: `(energyConsumedNet × 1000) / odometerDistance` → `xxx Wh/km` (둘 중 하나라도 null/0 이면 "-")

### Card 3: 속도 / 환경

| 필드 | 값 | 출처 |
|---|---|---|
| 평균 속도 | `speed_avg` km/h | `DriveDto.speedAvg` |
| 최고 속도 | `speed_max` km/h | `DriveDto.speedMax` |
| 외기온 | `outside_temp_avg` °C | `DriveDto.outsideTempAvg` |

> Note: `DriveDto` 에 시작/종료 주행계 분리값이 없으므로 `주행계` 라인은 생략. `odometerDistance` 로 충분.

## Charge Detail Screen — Contents

### Header

- 제목: `address` (충전 위치)
- 부제: 시작 일시
- 우측 상단: 닫기(X) 아이콘 버튼

### Card 1: 충전 요약

| 필드 | 값 | 출처 |
|---|---|---|
| 소요시간 | `duration_str` | `ChargeDto.durationStr` |
| 추가 에너지 | `chargeEnergyAdded` kWh | `ChargeDto.chargeEnergyAdded` |
| 평균 충전 출력 | `(chargeEnergyAdded × 60) / durationMin` kW (계산값) | 계산 |
| 사용 에너지 | `chargeEnergyUsed` kWh (null 이면 숨김) | `ChargeDto.chargeEnergyUsed` |

### Card 2: 배터리

- 좌: `startBatteryLevel` % 게이지 · 우: `endBatteryLevel` % 게이지
- `증가`: `(endBatteryLevel - startBatteryLevel)` %p

### Card 3: 비용 / 환경

| 필드 | 값 | 출처 |
|---|---|---|
| 비용 | `cost` 원 | `ChargeDto.cost` |
| kWh 당 단가 | `cost / chargeEnergyAdded` 원/kWh (계산값, 둘 중 하나라도 null/0 이면 "-") | 계산 |
| 외기온 | `outsideTempAvg` °C | `ChargeDto.outsideTempAvg` |
| 주행계 | `odometer` km | `ChargeDto.odometer` |

## Interactions

- **진입**: `DrivingScreen` / `ChargingScreen` 의 리스트 아이템 Row 에 이미 `Modifier.clickable` 가 있으므로 `onClick` 에 `vm.selectDrive(it.driveId)` 연결
- **닫기**: 닫기 버튼 or 뒤로가기 → `vm.clearSelection()`
- **스크롤**: 상세 내용이 길어지면 `LazyColumn` 또는 `verticalScroll(rememberScrollState())`
- **스와이프-to-close**: MVP 에서는 제외 (BatteryDetailScreen 도 버튼으로만 닫음)

## State Management

기존 ViewModel 에 얇게 추가:

```kotlin
// DrivingViewModel
private val _selectedDriveId = MutableStateFlow<Int?>(null)
val selectedDriveId: StateFlow<Int?> = _selectedDriveId.asStateFlow()

val selectedDrive: StateFlow<DriveDto?> =
    combine(drives, _selectedDriveId) { list, id ->
        if (id == null) null else list.firstOrNull { it.driveId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

fun selectDrive(id: Int?) { _selectedDriveId.value = id }
fun clearSelection() { _selectedDriveId.value = null }
```

`ChargingViewModel` 동일 패턴.

> 주의: 리스트가 새로고침되어도 선택 유지됨 (id 기반). 선택된 항목이 리스트에서 사라진 경우 `selectedDrive` 가 null 이 되어 오버레이 자동 dismiss.

## Styling

- 배경: `0xFF0B0B0B` (DarkBg, 기존과 동일)
- 카드: `0xFF1A1A1A` (CardBg), `RoundedCornerShape(16.dp)`
- 텍스트: `TextPrimary` / `TextSecondary` (기존 팔레트)
- 강조색: 주행은 `TeslaRed` (0xFFE31937), 충전은 `ChargingBlue` (0xFF00C7FF)
- 간격: 카드 간 `12.dp`, 카드 내부 `16.dp` 패딩

## Error / Edge Cases

- DTO 필드가 null 인 경우 → `-` 또는 해당 라인 숨김 (각 필드별로 명시)
- `startBatteryLevel == endBatteryLevel` 인 경우에도 게이지 정상 표시
- 비용 / 단가 계산 시 0 분모 방지 (`if (x == null || x == 0.0) "-" else …`)
- 선택된 id 에 해당하는 DTO 가 리스트에 없는 경우 → 오버레이 숨김 (null 처리)

## Testing Strategy

- 현재 프로젝트에 테스트 하네스가 구성되어 있지 않으므로, 수동 QA 로 진행:
  - 리스트에서 각 아이템 탭 → 오버레이 표시 확인
  - 닫기 버튼 동작 확인
  - null 필드 (예: `cost`, `energyConsumedNet`) 처리 확인 — 실제 TeslaMate 인스턴스의 과거 데이터에 null 있는 항목 선택
  - Android / iOS 양쪽 빌드 확인
- 향후 단위 테스트 추가 시 효율·단가 계산 유틸 분리 대상

## Future Enhancements (out of scope)

- 경로 지도 (proxy 서버 또는 positions 엔드포인트 확보 후)
- 충전 kW 곡선
- 비용 편집
- Tesla Nav 로 출발지 재-공유 (기존 `sendNavigationGps` 재사용 가능)
- 공유 버튼 (스크린샷 or 요약 텍스트 공유)
