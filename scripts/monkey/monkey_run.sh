#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

PACKAGE="com.bird.starryskysudoku"
EVENTS=50000
THROTTLE_MS=200
BATCH_SIZE=100
SEED=""
DEVICE=""
CAPTURE_BUGREPORT=1
AUTO_LAUNCH=1
OUTPUT_ROOT="$PROJECT_DIR/artifacts/monkey"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
FRESH_GRANT_MODE=0

usage() {
  cat <<'EOF'
Usage: scripts/monkey_run.sh [options]

Options:
  --device SERIAL           Target device serial. Defaults to the only connected device.
  --package NAME            Package name. Default: com.bird.starryskysudoku
  --events COUNT            Monkey event count. Default: 50000
  --throttle MS             Delay between events in milliseconds. Default: 200
  --batch-size COUNT        Monkey batch size before each safety check. Default: 100
  --seed VALUE              Monkey random seed. Default: current timestamp
  --apk PATH                APK path used for fresh install mode. Default: app/build/outputs/apk/release/app-release.apk
  --fresh-grant             Reinstall the APK, grant POST_NOTIFICATIONS, then start monkey
  --no-bugreport            Skip adb bugreport collection
  --no-launch               Do not force launch the app before monkey starts
  --output-root DIR         Output root directory. Default: artifacts/monkey
  -h, --help                Show this help message
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      DEVICE="$2"
      shift 2
      ;;
    --package)
      PACKAGE="$2"
      shift 2
      ;;
    --events)
      EVENTS="$2"
      shift 2
      ;;
    --throttle)
      THROTTLE_MS="$2"
      shift 2
      ;;
    --batch-size)
      BATCH_SIZE="$2"
      shift 2
      ;;
    --seed)
      SEED="$2"
      shift 2
      ;;
    --apk)
      APK_PATH="$2"
      shift 2
      ;;
    --fresh-grant)
      FRESH_GRANT_MODE=1
      shift
      ;;
    --no-bugreport)
      CAPTURE_BUGREPORT=0
      shift
      ;;
    --no-launch)
      AUTO_LAUNCH=0
      shift
      ;;
    --output-root)
      OUTPUT_ROOT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB+=( -s "$DEVICE" )
fi

if [[ -z "$SEED" ]]; then
  SEED="$(date +%s)"
fi

mkdir -p "$OUTPUT_ROOT"
RUN_DIR="$OUTPUT_ROOT/$(timestamp)"
mkdir -p "$RUN_DIR"
RUN_START_TIME="${RUN_DIR##*/}"
RUN_START_TIME="${RUN_START_TIME/_/ }"

MONKEY_LOG="$RUN_DIR/monkey.log"
LOGCAT_ALL="$RUN_DIR/logcat-all.txt"
LOGCAT_CRASH="$RUN_DIR/logcat-crash.txt"
LOGCAT_EVENTS="$RUN_DIR/logcat-events.txt"
DEVICE_INFO="$RUN_DIR/device-info.txt"
ACTIVITY_DUMPSYS="$RUN_DIR/dumpsys-activity.txt"
PACKAGE_DUMPSYS="$RUN_DIR/dumpsys-package.txt"
DROPBOX_DUMPSYS="$RUN_DIR/dumpsys-dropbox.txt"
BUGREPORT_FILE="$RUN_DIR/bugreport.zip"
SUMMARY_FILE="$RUN_DIR/summary.txt"
PREPARE_LOG="$RUN_DIR/device-prepare.txt"
WATCHDOG_LOG="$RUN_DIR/watchdog.txt"

ORIGINAL_SCREEN_OFF_TIMEOUT=""
ORIGINAL_STAY_ON=""
SWIPE_X1=0
SWIPE_Y1=0
SWIPE_X2=0
SWIPE_Y2=0
INTERVENTION_COUNT=0
PACKAGE_REGEX="${PACKAGE//./\\.}"

adb_shell() {
  "${ADB[@]}" shell "$@"
}

ensure_fresh_grant_prerequisites() {
  if [[ "$FRESH_GRANT_MODE" -eq 0 ]]; then
    return
  fi

  if [[ ! -f "$APK_PATH" ]]; then
    echo "APK not found: $APK_PATH" >&2
    exit 1
  fi
}

get_setting() {
  local namespace="$1"
  local key="$2"
  adb_shell settings get "$namespace" "$key" | tr -d '\r'
}

put_setting() {
  local namespace="$1"
  local key="$2"
  local value="$3"
  adb_shell settings put "$namespace" "$key" "$value" >/dev/null 2>&1 || true
}

compute_unlock_swipe() {
  local size_output width height
  size_output="$(adb_shell wm size | tr -d '\r')"
  if [[ "$size_output" =~ Override\ size:\ ([0-9]+)x([0-9]+) ]]; then
    width="${BASH_REMATCH[1]}"
    height="${BASH_REMATCH[2]}"
  elif [[ "$size_output" =~ Physical\ size:\ ([0-9]+)x([0-9]+) ]]; then
    width="${BASH_REMATCH[1]}"
    height="${BASH_REMATCH[2]}"
  else
    width=1080
    height=1920
  fi
  SWIPE_X1=$(( width / 2 ))
  SWIPE_Y1=$(( height * 8 / 10 ))
  SWIPE_X2=$(( width / 2 ))
  SWIPE_Y2=$(( height * 2 / 10 ))
}

is_screen_awake() {
  local power_dump
  power_dump="$(adb_shell dumpsys power | tr -d '\r' || true)"
  [[ "$power_dump" == *"mWakefulness=Awake"* ]]
}

is_keyguard_showing() {
  local policy_dump showing_value
  policy_dump="$(adb_shell dumpsys window policy | tr -d '\r' || true)"
  showing_value="$(awk -F= '/^[[:space:]]+showing=/{print $2; exit}' <<< "$policy_dump")"
  [[ "$showing_value" == "true" ]]
}

current_focus_line() {
  adb_shell dumpsys window | tr -d '\r' | grep -m 1 "mCurrentFocus=" || true
}

readiness_snapshot() {
  local focus_line awake_state keyguard_state foreground_state

  focus_line="$(current_focus_line)"
  if is_screen_awake; then
    awake_state=1
  else
    awake_state=0
  fi

  if is_keyguard_showing; then
    keyguard_state=1
  else
    keyguard_state=0
  fi

  if [[ -n "$focus_line" && "$focus_line" == *"$PACKAGE"* ]]; then
    foreground_state=1
  else
    foreground_state=0
  fi

  printf 'awake=%s keyguard=%s foreground=%s focus=%s\n' \
    "$awake_state" "$keyguard_state" "$foreground_state" "$focus_line"
}

bring_app_to_foreground() {
  adb_shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
}

unlock_device_if_needed() {
  if ! is_screen_awake; then
    adb_shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    sleep 1
  fi

  if is_keyguard_showing; then
    adb_shell input swipe "$SWIPE_X1" "$SWIPE_Y1" "$SWIPE_X2" "$SWIPE_Y2" 300 >/dev/null 2>&1 || true
    sleep 1
    adb_shell input keyevent KEYCODE_MENU >/dev/null 2>&1 || true
    sleep 1
  fi
}

restore_device_settings() {
  if [[ -n "$ORIGINAL_SCREEN_OFF_TIMEOUT" && "$ORIGINAL_SCREEN_OFF_TIMEOUT" != "null" ]]; then
    put_setting system screen_off_timeout "$ORIGINAL_SCREEN_OFF_TIMEOUT"
  fi
  if [[ -n "$ORIGINAL_STAY_ON" && "$ORIGINAL_STAY_ON" != "null" ]]; then
    put_setting global stay_on_while_plugged_in "$ORIGINAL_STAY_ON"
  fi
}

prepare_device() {
  compute_unlock_swipe
  ORIGINAL_SCREEN_OFF_TIMEOUT="$(get_setting system screen_off_timeout)"
  ORIGINAL_STAY_ON="$(get_setting global stay_on_while_plugged_in)"

  {
    echo "original_screen_off_timeout=$ORIGINAL_SCREEN_OFF_TIMEOUT"
    echo "original_stay_on_while_plugged_in=$ORIGINAL_STAY_ON"
    echo "unlock_swipe=${SWIPE_X1},${SWIPE_Y1}->${SWIPE_X2},${SWIPE_Y2}"
    echo "initial_focus=$(current_focus_line)"
  } > "$PREPARE_LOG"

  : > "$WATCHDOG_LOG"

  # 插电测试时强制常亮，并把息屏时间拉长到一小时，减少 monkey 误触发后的锁屏干扰。
  put_setting global stay_on_while_plugged_in 7
  put_setting system screen_off_timeout 3600000

  unlock_device_if_needed
}

device_ready_for_batch() {
  local snapshot
  snapshot="$(readiness_snapshot)"
  [[ "$snapshot" == awake=1\ keyguard=0\ foreground=1* ]]
}

recover_to_target_app() {
  local reason="$1"

  INTERVENTION_COUNT=$((INTERVENTION_COUNT + 1))
  {
    echo "$(timestamp) recover_reason=$reason"
    echo "$(timestamp) recover_state_before=$(readiness_snapshot)"
    echo "$(timestamp) recover_focus_before=$(current_focus_line)"
  } >> "$WATCHDOG_LOG"

  unlock_device_if_needed
  bring_app_to_foreground
  sleep 2

  {
    echo "$(timestamp) recover_state_after=$(readiness_snapshot)"
    echo "$(timestamp) recover_focus_after=$(current_focus_line)"
  } >> "$WATCHDOG_LOG"
}

cleanup() {
  local exit_code=$?
  if [[ -n "${LOGCAT_ALL_PID:-}" ]]; then
    kill "$LOGCAT_ALL_PID" 2>/dev/null || true
  fi
  if [[ -n "${LOGCAT_CRASH_PID:-}" ]]; then
    kill "$LOGCAT_CRASH_PID" 2>/dev/null || true
  fi
  if [[ -n "${LOGCAT_EVENTS_PID:-}" ]]; then
    kill "$LOGCAT_EVENTS_PID" 2>/dev/null || true
  fi
  restore_device_settings
  wait 2>/dev/null || true
  exit "$exit_code"
}
trap cleanup EXIT

resolve_device() {
  if [[ -n "$DEVICE" ]]; then
    return
  fi

  mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  if [[ "${#devices[@]}" -eq 0 ]]; then
    echo "No online adb device found." >&2
    exit 1
  fi
  if [[ "${#devices[@]}" -gt 1 ]]; then
    echo "Multiple adb devices found. Re-run with --device SERIAL." >&2
    printf 'Devices:\n%s\n' "${devices[@]}" >&2
    exit 1
  fi
  DEVICE="${devices[0]}"
  ADB=(adb -s "$DEVICE")
}

record_summary_line() {
  printf '%s\n' "$1" | tee -a "$SUMMARY_FILE"
}

collect_matches() {
  local pattern="$1"
  local source_file="$2"
  local label="$3"
  local count
  count=$(rg -c "$pattern" "$source_file" 2>/dev/null || true)
  count="${count:-0}"
  record_summary_line "$label: $count"
}

collect_package_matches() {
  local pattern="$1"
  local source_file="$2"
  local label="$3"
  local count
  count=$(rg -c "$pattern" "$source_file" 2>/dev/null || true)
  count="${count:-0}"
  record_summary_line "$label: $count"
}

collect_dropbox_package_entries() {
  local mode="$1"
  local label="$2"
  local count
  count=$(
    awk -v pkg="$PACKAGE" -v mode="$mode" -v start_time="$RUN_START_TIME" '
      $0 ~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9] / {
        entry_time = substr($0, 1, 19)
        if (entry_time < start_time) {
          tag = ""
          eligible = 0
          next
        }
        eligible = 1
      }
      index($0, " data_app_crash ") > 0 {
        if (!eligible) {
          tag = ""
          next
        }
        tag = "crash"
        next
      }
      index($0, " data_app_native_crash ") > 0 {
        if (!eligible) {
          tag = ""
          next
        }
        tag = "native"
        next
      }
      index($0, " data_app_anr ") > 0 {
        if (!eligible) {
          tag = ""
          next
        }
        tag = "anr"
        next
      }
      $0 ~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9] / {
        tag = ""
      }
      $0 ~ ("Process: " pkg "/") {
        if (mode == "crash" && (tag == "crash" || tag == "native")) {
          count++
          tag = ""
        } else if (mode == "anr" && tag == "anr") {
          count++
          tag = ""
        } else if (mode == "issue" && tag != "") {
          count++
          tag = ""
        }
      }
      END {
        print count + 0
      }
    ' "$DROPBOX_DUMPSYS"
  )
  count="${count:-0}"
  record_summary_line "$label: $count"
}

reinstall_and_grant_notifications() {
  if [[ "$FRESH_GRANT_MODE" -eq 0 ]]; then
    return
  fi

  {
    echo "fresh_grant_mode=1"
    echo "apk_path=$APK_PATH"
    echo "uninstall_existing=$("${ADB[@]}" shell pm list packages | tr -d '\r' | grep -q "^package:${PACKAGE}$" && echo yes || echo no)"
  } >> "$PREPARE_LOG"

  "${ADB[@]}" uninstall "$PACKAGE" >/dev/null 2>&1 || true
  "${ADB[@]}" install -r "$APK_PATH" >/dev/null

  if ! adb_shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1; then
    echo "grant_post_notifications=failed_or_not_required" >> "$PREPARE_LOG"
  else
    echo "grant_post_notifications=granted" >> "$PREPARE_LOG"
  fi
}

resolve_device
ensure_fresh_grant_prerequisites

{
  echo "timestamp=$(timestamp)"
  echo "device=$DEVICE"
  echo "package=$PACKAGE"
  echo "events=$EVENTS"
  echo "throttle_ms=$THROTTLE_MS"
  echo "batch_size=$BATCH_SIZE"
  echo "seed=$SEED"
  echo "fresh_grant_mode=$FRESH_GRANT_MODE"
  echo "apk_path=${APK_PATH:-<none>}"
} > "$SUMMARY_FILE"

{
  echo "=== adb get-state ==="
  "${ADB[@]}" get-state
  echo
  echo "=== ro.product ==="
  "${ADB[@]}" shell getprop ro.product.manufacturer
  "${ADB[@]}" shell getprop ro.product.model
  "${ADB[@]}" shell getprop ro.build.version.release
  "${ADB[@]}" shell getprop ro.build.version.sdk
} > "$DEVICE_INFO"

reinstall_and_grant_notifications

installed_packages="$("${ADB[@]}" shell pm list packages | tr -d '\r')"
if ! printf '%s\n' "$installed_packages" | grep -q "^package:${PACKAGE}$"; then
  echo "Package not installed on device: $PACKAGE" >&2
  exit 1
fi

{
  echo
  echo "=== package path ==="
  "${ADB[@]}" shell pm path "$PACKAGE"
} >> "$DEVICE_INFO"

prepare_device

"${ADB[@]}" logcat -c

if [[ "$AUTO_LAUNCH" -eq 1 ]]; then
  "${ADB[@]}" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 2
fi

if ! device_ready_for_batch; then
  recover_to_target_app "初始状态不是目标应用前台或仍处于锁屏状态"
fi

"${ADB[@]}" logcat -v threadtime > "$LOGCAT_ALL" &
LOGCAT_ALL_PID=$!

"${ADB[@]}" logcat -v threadtime AndroidRuntime:E DEBUG:F ActivityManager:E WindowManager:E System.err:W libc:F crash_dump64:E crash_dump32:E *:S > "$LOGCAT_CRASH" &
LOGCAT_CRASH_PID=$!

"${ADB[@]}" logcat -v threadtime ActivityTaskManager:I ActivityManager:I WindowManager:I monkey:I *:S > "$LOGCAT_EVENTS" &
LOGCAT_EVENTS_PID=$!

record_summary_line "运行目录: $RUN_DIR"
record_summary_line "运行状态: 运行中"
record_summary_line "自动恢复次数: 0"

remaining_events="$EVENTS"
batch_index=0
{
  echo "=== monkey batches ==="
  while [[ "$remaining_events" -gt 0 ]]; do
    if ! device_ready_for_batch; then
      recover_to_target_app "检测到锁屏、熄屏或前台焦点已离开目标应用"
    fi

    current_batch="$BATCH_SIZE"
    if [[ "$remaining_events" -lt "$current_batch" ]]; then
      current_batch="$remaining_events"
    fi
    batch_index=$((batch_index + 1))
    current_seed=$((SEED + batch_index - 1))

    echo
    echo "=== batch $batch_index ==="
    echo "events=$current_batch seed=$current_seed remaining_before=$remaining_events"

    "${ADB[@]}" shell monkey \
      -p "$PACKAGE" \
      --throttle "$THROTTLE_MS" \
      --pct-touch 100 \
      --pct-motion 0 \
      --pct-nav 0 \
      --pct-majornav 0 \
      --pct-appswitch 0 \
      --pct-syskeys 0 \
      --pct-trackball 0 \
      --pct-anyevent 0 \
      --ignore-crashes \
      --ignore-timeouts \
      --ignore-security-exceptions \
      --monitor-native-crashes \
      --kill-process-after-error \
      -s "$current_seed" \
      -v -v \
      "$current_batch" || true

    remaining_events=$((remaining_events - current_batch))
    echo "remaining_after=$remaining_events"
  done
} > "$MONKEY_LOG" 2>&1

sleep 3

"${ADB[@]}" shell dumpsys activity > "$ACTIVITY_DUMPSYS" || true
"${ADB[@]}" shell dumpsys package "$PACKAGE" > "$PACKAGE_DUMPSYS" || true
"${ADB[@]}" shell dumpsys dropbox > "$DROPBOX_DUMPSYS" || true

if [[ "$CAPTURE_BUGREPORT" -eq 1 ]]; then
  if "${ADB[@]}" bugreport "$BUGREPORT_FILE" >/dev/null 2>&1; then
    record_summary_line "Bugreport: $BUGREPORT_FILE"
  else
    record_summary_line "Bugreport: 抓取失败"
  fi
else
  record_summary_line "Bugreport: 已跳过"
fi

collect_dropbox_package_entries "crash" "崩溃/原生崩溃"
collect_dropbox_package_entries "anr" "ANR 无响应"
collect_package_matches "Process ${PACKAGE_REGEX} \\(pid .* has died" "$LOGCAT_ALL" "进程死亡"
collect_dropbox_package_entries "issue" "Dropbox 问题条目"
collect_matches "Events injected" "$MONKEY_LOG" "Monkey 事件注入批次数"
record_summary_line "自动恢复次数: $INTERVENTION_COUNT"

if [[ "$remaining_events" -eq 0 ]]; then
  record_summary_line "运行状态: 已完成"
else
  record_summary_line "运行状态: 中断或异常退出"
fi

record_summary_line "产物就绪: 是"

echo
echo "Monkey run complete."
echo "Artifacts: $RUN_DIR"
