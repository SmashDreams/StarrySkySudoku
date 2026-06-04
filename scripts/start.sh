#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.bird.starryskysudoku"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ──────────────────────────────────────────────
# 依赖检查
# ──────────────────────────────────────────────
check_deps() {
    local missing=()

    if ! command -v adb &>/dev/null; then
        missing+=("adb (Android SDK Platform Tools)")
    fi

    if ! command -v python3 &>/dev/null; then
        missing+=("python3")
    fi

    python3 -c "import cv2" 2>/dev/null || missing+=("opencv-python (pip install opencv-python)")
    python3 -c "import numpy" 2>/dev/null || missing+=("numpy (pip install numpy)")

    if [[ ${#missing[@]} -gt 0 ]]; then
        echo -e "${RED}缺少以下依赖：${NC}"
        for dep in "${missing[@]}"; do
            echo -e "  ${YELLOW}✗${NC} $dep"
        done
        echo ""
        echo "请先安装后重试："
        for dep in "${missing[@]}"; do
            case "$dep" in
                adb*) echo "  安装 Android SDK Platform Tools 并添加到 PATH" ;;
                python3*) echo "  apt install python3  (或从 https://python.org 下载)" ;;
                opencv*) echo "  pip install opencv-python" ;;
                numpy*) echo "  pip install numpy" ;;
            esac
        done
        exit 1
    fi
}

# ──────────────────────────────────────────────
# 设备选择
# ──────────────────────────────────────────────
select_device() {
    local serials_file
    serials_file=$(mktemp)
    adb devices 2>/dev/null | tail -n +2 | awk '{print $1}' | grep -v '^$' > "$serials_file"

    local count
    count=$(grep -c . "$serials_file" || true)

    if [[ $count -eq 0 ]]; then
        rm -f "$serials_file"
        echo -e "${RED}未检测到已连接的设备${NC}"
        echo ""
        echo "请通过 USB 连接设备并开启 USB 调试，然后按回车重试…"
        read -r
        select_device
        return
    fi

    if [[ $count -eq 1 ]]; then
        DEVICE=$(head -1 "$serials_file")
        rm -f "$serials_file"
        echo -e "自动选择设备: ${GREEN}$DEVICE${NC}"
        return
    fi

    # 多设备：用进程替换避免 adb shell 吃掉 stdin
    echo "检测到多个设备："
    local i=1
    while IFS= read -r serial; do
        local model
        model=$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null < /dev/null | tr -d '\r' || echo "unknown")
        echo "  [$i] $serial  ($model)"
        i=$((i + 1))
    done < "$serials_file"

    while true; do
        echo ""
        read -rp "请选择设备 (1-$count): " choice
        if [[ "$choice" =~ ^[0-9]+$ ]] && [[ "$choice" -ge 1 ]] && [[ "$choice" -le $count ]]; then
            break
        fi
        echo -e "${RED}请输入 1 到 $count 之间的数字${NC}"
    done

    DEVICE=$(sed -n "${choice}p" "$serials_file")
    rm -f "$serials_file"
    echo -e "已选择: ${GREEN}$DEVICE${NC}"
}

# ──────────────────────────────────────────────
# 检查 APK
# ──────────────────────────────────────────────
check_apk() {
    if [[ ! -f "$APK" ]]; then
        echo -e "${RED}APK 不存在: $APK${NC}"
        echo "请先构建项目：cd $PROJECT_DIR && ./gradlew assembleDebug"
        exit 1
    fi
}

# ──────────────────────────────────────────────
# Monkey 测试
# ──────────────────────────────────────────────
run_monkey() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  Monkey 随机压测${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # 是否已安装
    local installed=false
    if adb -s "$DEVICE" shell pm list packages 2>/dev/null | grep -q "$PKG"; then
        echo -e "应用 ${GREEN}已安装${NC}"
        echo ""
        echo "是否重新安装？"
        echo "  [1] 已安装，直接压测"
        echo "  [2] 重新安装后再压测"

        while true; do
            read -rp "请选择 (1/2): " reinstall_choice
            reinstall_choice="${reinstall_choice:-1}"
            if [[ "$reinstall_choice" == "1" || "$reinstall_choice" == "2" ]]; then
                break
            fi
            echo -e "${RED}请输入 1 或 2${NC}"
        done

        if [[ "$reinstall_choice" == "2" ]]; then
            installed=false
        else
            installed=true
        fi
    else
        echo -e "应用 ${YELLOW}未安装${NC}，将先安装再压测"
        installed=false
    fi

    # 批次数
    echo ""
    echo "请选择压测批次（每批 50 个随机点击事件）："
    echo "  [1] 轻量  —   500 事件 (10 批)"
    echo "  [2] 标准  —  2000 事件 (40 批)"
    echo "  [3] 深度  —  5000 事件 (100 批)"
    echo "  [4] 自定义"

    local events
    while true; do
        read -rp "请选择 (1/2/3/4): " batch_choice
        batch_choice="${batch_choice:-1}"
        case "$batch_choice" in
            1) events=500; break ;;
            2) events=2000; break ;;
            3) events=5000; break ;;
            4)
                while true; do
                    read -rp "请输入事件总数: " events
                    if [[ "$events" =~ ^[0-9]+$ ]] && [[ "$events" -ge 1 ]]; then
                        break
                    fi
                    echo -e "${RED}请输入大于 0 的整数${NC}"
                done
                break
                ;;
            *)
                echo -e "${RED}请输入 1、2、3 或 4${NC}"
                ;;
        esac
    done

    # 构建命令
    local fresh_flag=""
    if [[ "$installed" == false ]]; then
        fresh_flag=" --fresh-grant"
    fi

    echo ""
    echo -e "设备:       ${GREEN}$DEVICE${NC}"
    echo -e "安装模式:   ${GREEN}$([[ "$installed" == true ]] && echo "直接压测" || echo "卸载重装")${NC}"
    echo -e "事件总数:   ${GREEN}$events${NC}"
    echo -e "事件间隔:   ${GREEN}80ms${NC}"
    echo -e "批次大小:   ${GREEN}50${NC}"
    echo ""

    local cmd="$SCRIPT_DIR/monkey/monkey_run.sh --device $DEVICE --apk $APK --events $events --throttle 80 --batch-size 50 --no-bugreport$fresh_flag"
    if $cmd; then
        echo ""
        echo -e "${GREEN}Monkey 压测完成${NC}"
    else
        echo ""
        echo -e "${RED}Monkey 压测异常退出${NC}"
        exit 1
    fi
}

# ──────────────────────────────────────────────
# Full Playthrough 测试
# ──────────────────────────────────────────────
run_full_playthrough() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  全量真人式通关${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # 生成答案
    local data_file="$SCRIPT_DIR/full_playthrough/data/level_solutions.json"
    if [[ ! -f "$data_file" ]]; then
        echo -e "${YELLOW}答案文件不存在，正在生成…${NC}"
        python3 "$SCRIPT_DIR/full_playthrough/generate_level_solutions.py"
        echo ""
    fi

    # 结束关卡
    echo "请选择通关范围："
    echo "  [1] 全部 40 关"
    echo "  [2] 自定义结束关卡"

    local end_level
    while true; do
        read -rp "请选择 (1/2): " range_choice
        range_choice="${range_choice:-1}"
        if [[ "$range_choice" == "1" ]]; then
            end_level=40
            break
        elif [[ "$range_choice" == "2" ]]; then
            while true; do
                read -rp "请输入结束关卡 (1-40): " end_level
                if [[ "$end_level" =~ ^[0-9]+$ ]] && [[ "$end_level" -ge 1 ]] && [[ "$end_level" -le 40 ]]; then
                    break
                fi
                echo -e "${RED}请输入 1 到 40 之间的整数${NC}"
            done
            break
        else
            echo -e "${RED}请输入 1 或 2${NC}"
        fi
    done

    echo ""
    echo -e "设备:       ${GREEN}$DEVICE${NC}"
    echo -e "通关范围:   ${GREEN}第 1 关 — 第 $end_level 关${NC}"
    echo ""

    local cmd="python3 $SCRIPT_DIR/full_playthrough/runner.py --device $DEVICE --start-level 1 --end-level $end_level"
    if $cmd; then
        echo ""
        echo -e "${GREEN}全量通关完成${NC}"
    else
        echo ""
        echo -e "${RED}通关脚本异常退出${NC}"
        exit 1
    fi
}

# ──────────────────────────────────────────────
# 主菜单
# ──────────────────────────────────────────────
main() {
    clear 2>/dev/null || true

    echo ""
    echo -e "${CYAN}================================================${NC}"
    echo -e "${CYAN}     StarrySkySudoku 自动化测试脚本${NC}"
    echo -e "${CYAN}================================================${NC}"
    echo ""

    check_deps
    check_apk

    echo "正在检查设备连接…"
    select_device
    echo ""

    echo "请选择测试类型："
    echo "  [1] Monkey 随机压测"
    echo "  [2] Full Playthrough 全量通关"
    echo "  [3] 退出"
    echo ""

    while true; do
        read -rp "请选择 (1/2/3): " choice
        case "$choice" in
            1) run_monkey; break ;;
            2) run_full_playthrough; break ;;
            3) echo "已退出"; exit 0 ;;
            *) echo -e "${RED}请输入 1、2 或 3${NC}" ;;
        esac
    done
}

main "$@"
