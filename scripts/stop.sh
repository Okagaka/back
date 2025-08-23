#!/usr/bin/env bash

# =============================
# 환경 설정
# =============================
#PROJECT_ROOT="/home/ubuntu/apps/spring-practice"
#JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"
PROJECT_ROOT="/home/ubuntu/apps/okagaka-server"
#JAR_FILE="$PROJECT_ROOT/build/libs/okagaka-server-0.0.1-SNAPSHOT.jar"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

# 디렉터리와 로그 파일 생성
mkdir -p "$PROJECT_ROOT"
touch "$DEPLOY_LOG"

TIME_NOW=$(date +%c)

# =============================
# 현재 구동 중인 애플리케이션 pid 확인
# =============================
#CURRENT_PID=$(pgrep -f "$JAR_FILE")
CURRENT_PID=$(pgrep -f "$PROJECT_ROOT/build/libs/.*\.jar")

if [ -z "$CURRENT_PID" ]; then
  echo "$TIME_NOW > 현재 실행 중인 애플리케이션이 없습니다" >> "$DEPLOY_LOG"
else
  echo "$TIME_NOW > 실행 중인 PID $CURRENT_PID 애플리케이션 종료" >> "$DEPLOY_LOG"
  kill -15 "$CURRENT_PID"
  # 종료 대기 (최대 10초)
  for i in {1..10}; do
    if ps -p "$CURRENT_PID" > /dev/null; then
      sleep 1
    else
      echo "$TIME_NOW > 애플리케이션 종료 완료" >> "$DEPLOY_LOG"
      break
    fi
  done
fi


##!/usr/bin/env bash
#
#PROJECT_ROOT="/home/ubuntu/apps/spring-practice"
#JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"
#
#DEPLOY_LOG="$PROJECT_ROOT/deploy.log"
#
#TIME_NOW=$(date +%c)
#
## 현재 구동 중인 애플리케이션 pid 확인
#CURRENT_PID=$(pgrep -f $JAR_FILE)
#
## 프로세스가 켜져 있으면 종료
#if [ -z $CURRENT_PID ]; then
#  echo "$TIME_NOW > 현재 실행중인 애플리케이션이 없습니다" >> $DEPLOY_LOG
#else
#  echo "$TIME_NOW > 실행중인 $CURRENT_PID 애플리케이션 종료 " >> $DEPLOY_LOG
#  kill -15 $CURRENT_PID
#fi