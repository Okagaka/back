#!/usr/bin/env bash

# =============================
# 환경 설정
# =============================
PROJECT_ROOT="/home/ubuntu/apps/okagaka-server"
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

export $(cat /home/ubuntu/apps/okagaka-server/.env | xargs)

# DB, AWS, 기타 환경 변수 전달
nohup java \
  -Duser.timezone=Asia/Seoul \
  -DRDS_HOST=$RDS_HOST \
  -DRDS_PORT=$RDS_PORT \
  -DRDS_NAME=$RDS_NAME \
  -DRDS_USER=$RDS_USER \
  -DRDS_PASSWORD=$RDS_PASSWORD \
  -DAWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  -DAWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -DS3_BUCKET=$S3_BUCKET \
  -DTMAP_KEY=$TMAP_KEY \
  -DJWT_SECRET=$JWT_SECRET \
  -DJWT_EXPIRATION=$JWT_EXPIRATION \
  -DGOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS \
  -DFRONTEND_ORIGIN=$FRONTEND_ORIGIN \
  -jar "$JAR_FILE" > "$PROJECT_ROOT/nohup.out" 2>&1 &

sleep 5

NEW_PID=$(pgrep -f "$JAR_FILE")
if [ -z "$NEW_PID" ]; then
  echo "$TIME_NOW > 애플리케이션 시작 실패" >> "$DEPLOY_LOG"
else
  echo "$TIME_NOW > 애플리케이션 시작 성공 PID $NEW_PID" >> "$DEPLOY_LOG"
fi
