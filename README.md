# 알림 기록 (Notification Log)

안드로이드 기기에 도착한 **알림**을 기기 내부에 저장하고, 검색·분류·필터링·보관 기간 관리·엑셀(XLSX) 내보내기를 제공하는 로컬 중심 알림 기록 앱입니다.

> 이 앱은 카카오톡 등 메신저의 **대화 전체를 백업하는 앱이 아닙니다.**
> 각 앱이 사용자에게 표시한 "알림"의 제목·본문만 기록합니다.

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 알림 수집 | `NotificationListenerService`로 수신 시각, 앱 이름, 패키지명(아이콘 식별), 제목, 본문, 카테고리/채널을 저장합니다. 내용이 비어 있어도 앱 이름과 시각은 기록합니다. |
| 중복 방지 | `패키지명 + 제목 + 본문 + 알림 시각` SHA-256 해시를 고유 인덱스로 사용합니다. 같은 내용이 2초 안에 다시 게시되어도 저장하지 않습니다. 그룹 요약 알림은 제외합니다. |
| 알림 목록 | 최신순으로 표시하고 Paging 3로 무한 스크롤합니다. 앱·기간·키워드·사용자 필터·즐겨찾기 조건으로 거를 수 있습니다. 길게 누르면 즐겨찾기 또는 삭제(실행 취소 가능) 메뉴가 열립니다. |
| 검색 | 제목·본문에서 검색하며, 여러 단어는 공백으로 구분해 AND 조건으로 찾습니다. 앱·기간으로 범위를 좁힐 수 있고 검색어를 강조 표시합니다. |
| 앱별 기록 설정 | 모든 앱 기록(기본), 선택한 앱만 기록, 선택한 앱 제외 중에서 고릅니다. 설치된 앱을 검색해 선택할 수 있고, 설정은 Room에 저장됩니다. |
| 사용자 필터 | 이름, 여러 키워드, 대상 앱, 활성화 여부를 가집니다. 기본 필터로 배송·결제·예약·인증을 제공하며 추가·수정·삭제할 수 있습니다. |
| 보관 기간 | 7일, 30일(기본), 90일, 6개월, 1년, 무기한 중에서 고릅니다. 앱 실행 시와 WorkManager로 하루 1회 만료된 알림을 정리합니다. 즐겨찾기한 알림은 보존됩니다. |
| XLSX 내보내기 | 현재 화면의 조건(전체, 검색 결과, 앱/기간/필터)을 SAF로 지정한 위치에 저장합니다. 백그라운드에서 처리하며 진행 표시를 보여 줍니다. |
| 기타 | Material 3, 다크 모드, Android 12+ 동적 색상, 한국어 UI를 지원합니다. |

## 화면 구성

1. **알림**(홈): 권한 안내 카드, 조건 칩, 알림 목록, 내보내기
2. **검색**: 키워드 검색과 앱·기간 범위 지정, 결과 내보내기
3. **필터**: 필터 관리. 필터를 누르면 해당 알림 목록이 열립니다.
4. **설정**: 알림 권한 상태, 앱별 기록 설정, 진행 중 알림 제외, 보관 기간, 전체 내보내기, 데이터 삭제, 앱 정보
5. **알림 상세**: 전체 제목·본문(복사 가능), 앱 정보, 수신 시각, 카테고리, 원본 앱 열기

## 알림 접근 권한 허용 방법

알림을 읽으려면 **"알림 접근"** 특별 권한이 필요하며, 사용자가 시스템 설정에서 직접 허용해야 합니다.

1. 앱을 처음 실행하면 개인정보 안내가 표시됩니다. **권한 설정으로 이동**을 누르세요.
2. 시스템의 "알림 접근 허용"(기기에 따라 "기기 및 앱 알림") 화면에서 **알림 기록**을 켭니다.
3. 앱으로 돌아오면 권한 상태가 자동으로 갱신됩니다.

### 토글이 회색으로 비활성화되어 켜지지 않는 경우 (Android 13+)

GitHub Actions에서 받은 APK처럼 스토어 밖에서 설치한 앱은 Android 13부터 **제한된 설정**이 적용됩니다.

1. **설정 → 애플리케이션 → 알림 기록**으로 이동합니다.
2. 오른쪽 위 **⋮** 메뉴에서 **제한된 설정 허용**을 누르고 본인 인증을 합니다.
3. 다시 알림 접근 화면에서 **알림 기록**을 켭니다.

### 참고

- Android 13 이상에서 "알림 **표시** 권한(POST_NOTIFICATIONS)"과 "알림 **접근** 권한"은 서로 다른 권한입니다. 이 앱은 알림을 띄우지 않으므로 알림 표시 권한은 요청하지 않습니다.
- Android 15부터는 시스템이 인증번호(OTP) 같은 민감한 알림 내용을 일반 리스너 앱에 숨길 수 있습니다. 이 경우 본문이 빈 채로 기록됩니다.
- 일부 앱은 내용을 숨기거나(잠금 화면 민감 정보 숨김) 커스텀 뷰를 사용합니다. 이런 알림은 "(표시할 내용이 없는 알림)"으로 기록됩니다.

## 서비스 중단(배터리 최적화) 안내

이 앱은 별도의 포그라운드 서비스 없이, 시스템이 관리하는 `NotificationListenerService`로 동작합니다. 그래도 일부 제조사(삼성, 샤오미, 화웨이 등)의 배터리 최적화나 앱 자동 정리 때문에 수집이 멈출 수 있습니다.

- **설정 → 애플리케이션 → 알림 기록 → 배터리**에서 **제한 없음**(또는 최적화하지 않음)을 선택하세요.
- 삼성: **설정 → 배터리 → 백그라운드 사용 제한**에서 "절전 상태 앱" 목록에 이 앱이 없는지 확인하세요.
- 최근 앱 화면에서 앱을 잠그면(고정하면) 정리 대상에서 제외됩니다.
- 수집이 멈춘 것 같으면 앱을 한 번 열어 주세요. 앱은 화면에 돌아올 때 서비스 재연결(`requestRebind`)을 요청합니다.

## GitHub Actions로 APK 받기

`main` 브랜치에 push하거나 Actions 탭에서 수동 실행(workflow_dispatch)하면 debug APK가 빌드됩니다.

1. GitHub 저장소 → **Actions** 탭
2. 왼쪽에서 **Android Build** 워크플로 선택 → 가장 최근 실행(초록색 체크) 클릭
3. 페이지 아래 **Artifacts** → **android-debug-apk** 다운로드 (zip)
4. zip을 풀면 나오는 `app-debug.apk`를 휴대폰에 옮겨 설치합니다. 출처를 알 수 없는 앱 설치를 허용해야 할 수 있습니다.

수동 실행: Actions → Android Build → **Run workflow** → 브랜치 선택 → Run workflow

빌드가 실패하면 실행 페이지의 **Build debug APK** 단계 로그(`--stacktrace` 포함)를 확인하세요. Artifacts의 **build-logs**에서 전체 로그(`build-log.txt`)와 리포트를 받을 수 있습니다.

## 로컬 빌드

요구 사항: JDK 17 이상, Android SDK(Platform 36). Android Studio 최신 안정 버전을 권장합니다.

1. Android Studio → **Open** → 이 저장소 폴더 선택 (Gradle 동기화가 자동으로 실행됩니다)
2. 기기를 연결하고 ▶ Run을 누르거나, 터미널에서 다음을 실행합니다.

```bash
./gradlew assembleDebug
# 결과물: app/build/outputs/apk/debug/app-debug.apk
```

### 버전 구성

| 항목 | 버전 |
| --- | --- |
| Gradle | 8.14.3 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin / KSP | 2.2.21 / 2.2.21-2.0.4 |
| Compose BOM | 2025.08.00 |
| Room / Paging / WorkManager | 2.7.2 / 3.3.6 / 2.10.3 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |

버전은 `gradle/libs.versions.toml`에서 한곳에서 관리합니다.

## 패키지명 변경 (배포 전 필수)

기본 패키지명 `com.example.notificationlog`는 예시용이며 Google Play에 올릴 수 없습니다. 배포 전에 바꾸세요.

1. `app/build.gradle.kts`의 `namespace`와 `applicationId`를 원하는 값(예: `com.yourname.notificationlog`)으로 바꿉니다.
2. Android Studio에서 `app/src/main/java/com/example/notificationlog` 패키지를 우클릭 → **Refactor → Rename**으로 소스 패키지를 바꿉니다. 모든 `package`/`import` 선언이 함께 바뀝니다.
3. `applicationId`가 바뀌면 다른 앱으로 인식되므로 기존 기록은 옮겨지지 않습니다.

## XLSX 내보내기 사용법

1. **알림**, **검색**, **필터 결과** 화면에서 원하는 조건(앱·기간·키워드·필터·즐겨찾기)을 적용합니다.
2. 오른쪽 위 **내보내기(공유 모양) 아이콘**을 누릅니다. 전체를 내보내려면 **설정 → 전체 알림 내보내기**를 사용하세요.
3. 시스템 파일 선택 화면에서 저장 위치를 고르고 저장합니다. 기본 파일명은 `notification_log_20261027_0744.xlsx` 형식입니다.
4. 완료되면 저장 건수와 저장 위치가 표시됩니다.

엑셀 컬럼: **수신 시각 / 앱 이름 / 패키지명 / 제목 / 내용 / 카테고리 / 즐겨찾기(Y/N)**

엑셀 파일은 Apache POI 대신 내장 경량 XLSX 작성기(`export/XlsxExporter.kt`)로 만듭니다. POI는 Android에서 의존성 문제가 있고 APK 크기도 커집니다. 내장 작성기는 행을 하나씩 ZIP 스트림에 바로 쓰므로 알림이 수십만 건이어도 메모리 사용량이 일정합니다. 생성된 파일은 Excel, Google 스프레드시트, LibreOffice에서 열 수 있습니다.

> 내보낸 파일에는 금융·인증번호 같은 민감한 알림이 포함될 수 있으니 보관과 공유에 주의하세요.

## 개인정보 및 데이터 보관

- 모든 알림은 **기기 내부 Room(SQLite) 데이터베이스에만** 저장됩니다.
- 앱에 **인터넷 권한이 없으므로** 어떤 데이터도 서버로 전송되지 않습니다.
- 클라우드 백업과 기기 간 전송에서도 데이터가 제외됩니다(`allowBackup=false`, data extraction rules).
- 사용자가 **명시적으로 내보내기**를 실행해 직접 고른 위치에 저장하기 전까지 데이터는 앱 밖으로 나가지 않습니다.
- 요청하는 권한은 알림 접근(시스템 설정에서 사용자가 허용)뿐입니다. 저장소 권한, `QUERY_ALL_PACKAGES` 같은 넓은 권한은 사용하지 않습니다.

## 프로젝트 구조

```
app/src/main/java/com/example/notificationlog/
├─ NotificationLogApp.kt        # Application: DI 컨테이너, 정리 작업 등록
├─ AppContainer.kt              # 수동 DI (Hilt 대신 단순 구성)
├─ MainActivity.kt
├─ data/
│  ├─ local/                    # Room Entity, DAO, AppDatabase(기본 필터 시드)
│  └─ repository/               # Notification/Filter/AppRule/Settings/InstalledApps, 동적 쿼리 빌더
├─ domain/Models.kt             # 도메인 모델, 조회 조건, 보관 기간, 기록 모드
├─ service/
│  ├─ NotificationListenerService.kt  # NotificationLogListenerService (알림 수신)
│  ├─ NotificationParser.kt     # 제목/본문/카테고리 추출, 아이콘 캐시
│  └─ NotificationAccess.kt     # 권한 상태, 설정 화면 이동, 재연결
├─ worker/CleanupWorker.kt      # 하루 1회 만료 알림 정리
├─ export/                      # XlsxExporter(스트리밍 XLSX), NotificationExporter(SAF)
└─ ui/                          # Compose 화면: home, search, filter, settings, detail, common, theme
```

아키텍처는 **MVVM + Repository**입니다. Compose 화면이 ViewModel(StateFlow, PagingData)을 구독하고, ViewModel은 Repository를 거쳐 Room DAO에 접근합니다.

검색에는 `LIKE '%키워드%'`를 사용합니다. SQLite FTS의 기본 토크나이저는 한국어 단어 중간 일치(예: "롯데택배"에서 "택배")를 지원하지 않기 때문입니다. 대신 `postedAt`, `(packageName, postedAt)`, `(isFavorite, postedAt)` 인덱스로 정렬과 앱·기간·즐겨찾기 조건 탐색 범위를 줄입니다.

## 향후 개선 아이디어

- **알림 그룹화**: 앱·대화방(제목) 단위로 묶어 보기
- **통계 화면**: 앱별·시간대별 알림 수 차트
- **백업/복원**: 암호화된 로컬 백업 파일로 내보내기와 가져오기
- **위젯**: 최근 알림, 즐겨찾기 홈 화면 위젯
- **알림 자동 분류**: 규칙 또는 온디바이스 모델로 결제·배송·인증 등을 자동 태깅
- 다국어 문자열 리소스 분리, 앱 잠금(생체 인증), 대량 선택 삭제
