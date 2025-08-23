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

# build/libs 안에서 가장 최신 jar 선택
JAR_FILE=$(ls -t $PROJECT_ROOT/build/libs/*.jar | head -n 1)

# =============================
# 애플리케이션 실행
# =============================
#echo "$TIME_NOW > 애플리케이션 시작" >> "$DEPLOY_LOG"
echo "$TIME_NOW > 배포할 JAR 파일: $JAR_FILE" >> "$DEPLOY_LOG"
nohup java -jar "$JAR_FILE" > "$PROJECT_ROOT/nohup.out" 2>&1 &
sleep 3

NEW_PID=$(pgrep -f "$JAR_FILE")
if [ -z "$NEW_PID" ]; then
  echo "$TIME_NOW > 애플리케이션 시작 실패" >> "$DEPLOY_LOG"
else
  echo "$TIME_NOW > 애플리케이션 시작 성공 PID $NEW_PID" >> "$DEPLOY_LOG"
fi


##!/usr/bin/env bash
#
#PROJECT_ROOT="/home/ubuntu/apps/spring-practice"
#JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"
#
#APP_LOG="$PROJECT_ROOT/application.log"
#ERROR_LOG="$PROJECT_ROOT/error.log"
#DEPLOY_LOG="$PROJECT_ROOT/deploy.log"
#
#TIME_NOW=$(date +%c)
#
## build 파일 복사
#echo "$TIME_NOW > $JAR_FILE 파일 복사" >> $DEPLOY_LOG
#cp $PROJECT_ROOT/build/libs/*.jar $JAR_FILE
#
## jar 파일 실행
#echo "$TIME_NOW > $JAR_FILE 파일 실행" >> $DEPLOY_LOG
#nohup java -jar $JAR_FILE > $APP_LOG 2> $ERROR_LOG &
#
#CURRENT_PID=$(pgrep -f $JAR_FILE)
#echo "$TIME_NOW > 실행된 프로세스 아이디 $CURRENT_PID 입니다." >> $DEPLOY_LOG