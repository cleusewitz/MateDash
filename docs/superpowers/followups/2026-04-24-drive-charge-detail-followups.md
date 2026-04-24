# Drive / Charge Detail — Follow-up Tasks

_Date: 2026-04-24_
_Origin:_ Final code review of `e9fd15c...08d008c` (후속 리뷰 지적 사항)

초기 MVP 구현은 shipped (헤더·nullable 가드 수정 포함). 아래는 릴리즈 블로커는 아니지만 후속으로 정리할 항목들.

## 1. 공통 컴포넌트 / 유틸 추출 (MEDIUM)

`DriveDetailScreen.kt` 와 `ChargeDetailScreen.kt` 에 동일 코드가 복사되어 있음.

**중복 대상:**
- `Int.withComma()` (천 단위 콤마)
- `Double.fmt1()` (소수점 1자리)
- `Double.fmt0()` (정수 반올림 + 콤마)
- `formatDateTime(dateStr: String?)` (ISO → `yyyy-MM-dd HH:mm`)
- `DetailCard(title, accent, content)` composable
- `DetailRow(label, value)` composable
- `BatteryPill(label, percent)` composable

**제안:**
- `ui/common/FormatUtils.kt` 에 숫자/날짜 포맷 유틸 이동
- `ui/common/DetailComponents.kt` 에 `DetailCard`, `DetailRow`, `BatteryPill` 이동
- 두 detail 화면에서 import 해서 재사용
- `DashboardScreen.kt` 에도 유사한 private `DetailCard`/`DetailRow` 있음 — 같이 합치는 것 검토

**이슈:**
- `DashboardScreen` 의 `DetailCard` 는 `accent` 파라미터 없음 → 시그니처 호환성 확인 필요

## 2. `SharingStarted.Eagerly` → `WhileSubscribed(5_000)` (MEDIUM)

**위치:**
- `DrivingViewModel.kt:30` `selectedDrive`
- `ChargingViewModel.kt:44` `selectedCharge`

**현재:**
```kotlin
.stateIn(viewModelScope, SharingStarted.Eagerly, null)
```

**변경:**
```kotlin
.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

**이유:**
- `Eagerly` 는 옵저버 없어도 flow 가 항상 hot 상태. 현재 ViewModel 이 composable 에 스코프돼 있어 리크는 아니지만, lifecycle 베스트 프랙티스상 `WhileSubscribed` 가 적절.
- 5 초 그레이스 기간 덕에 configuration change (로테이션) 때도 정상 동작.

**검증 필요:**
- 탭 전환으로 화면 떠날 때 `selectedDrive` 가 null 로 리셋되는지 확인 (`WhileSubscribed` 는 초기값 사용됨).

## 3. `selectedDriveId` / `selectedChargeId` public StateFlow 미사용 (MEDIUM)

**위치:**
- `DrivingViewModel.kt:26`
- `ChargingViewModel.kt:42`

현재 public 으로 노출돼 있지만 어떤 composable 도 수집하지 않음. `selectedDrive` / `selectedCharge` 만 사용.

**옵션 A:** public val 삭제 (private 백킹 필드만 유지)
```kotlin
private val _selectedDriveId = MutableStateFlow<Int?>(null)
// val selectedDriveId: StateFlow<Int?> = _selectedDriveId.asStateFlow()  ← 제거
```

**옵션 B:** `internal` 로 강등하고 주석으로 향후 용도 명시

**추천:** A. YAGNI.

## 4. 미사용 `Box` import 삭제 (LOW)

**위치:**
- `DriveDetailScreen.kt:5` — `import androidx.compose.foundation.layout.Box`
- `ChargeDetailScreen.kt:5` — 동일

파일 내에서 `Box` 가 실제로 쓰이지 않음 (LazyColumn + Row 만 사용). import 정리.

## 5. `speedMax` 포맷 일관성 (LOW, 취향)

**위치:** `DriveDetailScreen.kt` 속도/환경 카드

- `speedAvg: Double?` → `it.fmt0()` (콤마 포함)
- `speedMax: Int?` → `it.withComma()` (콤마 포함)

둘 다 정상 동작 / 동일 결과지만 호출 경로가 다름. `speedMax.toString()` 으로 심플화 해도 무방 (Int 는 천 단위 안 넘을 가능성 높음).

우선순위 낮음. 스킵 가능.

## 6. 카드 모서리/패딩 스펙과 구현 차이 (MINOR DEVIATION)

리뷰어 지적:
- 스펙: `RoundedCornerShape(16.dp)` / 내부 패딩 `16.dp`
- 구현: `RoundedCornerShape(20.dp)` / 내부 패딩 `20.dp`

**판단:**
- `DashboardScreen.BatteryDetailScreen` 이 이미 `20.dp` / `20.dp` 를 사용 → 앱 내 일관성상 구현값이 맞음
- 스펙을 구현값으로 맞춰 업데이트 (스펙 문서 수정) 하는 쪽이 실용적

**액션:** 스펙 문서의 카드 모서리/패딩 수치를 `20.dp` 로 정정.

## 7. 향후 기능 확장 (참고용, 즉시 작업 아님)

스펙의 "Future Enhancements" 섹션 재확인 — 다음 iteration 후보:

- **주행 경로 지도** — TeslaMate PostgreSQL positions 테이블 조회용 프록시 서버 구축 후 polyline 렌더링
- **충전 kW 곡선** — 시계열 데이터 확보 방법 조사 (동일 프록시)
- **충전 비용 편집** — TeslaMate 쓰기 API 또는 로컬 오버라이드
- **Tesla 내비로 출발지 다시 전송** — `TeslaFleetApiClient.sendNavigationGps` 재사용
- **공유 버튼** — 요약 텍스트 / 스크린샷 공유

## 작업 순서 추천

저비용 → 고비용 순:

1. **#4 미사용 import 정리** (1분)
2. **#5 speedMax 포맷** (스킵해도 됨)
3. **#3 public StateFlow 삭제** (2분)
4. **#2 SharingStarted 변경** (2분 + 동작 검증 5분)
5. **#1 공통 컴포넌트/유틸 추출** (30분-1시간, 가장 큰 작업)
6. **#6 스펙 문서 정정** (5분)

#1 은 `DashboardScreen` 까지 정리 범위에 넣을지 먼저 결정.
