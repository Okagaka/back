# 🚗 OkaGaka (오카가카) - 가족 간 차량 공유 AI 플랫폼

> **AI 기반 가족 구성원 스케줄링 및 실시간 카풀 매칭 서비스**

OkaGaka는 가족 구성원들의 이동 수요를 분석하여 최적의 카풀 경로를 제안하고, 실시간 차량 위치를 공유하여 가족 간의 안전하고 효율적인 이동을 돕는 플랫폼입니다.

## 🌟 Key Features

* **👨‍👩‍👧‍👦 가족 그룹 관리**: 가족 구성원을 그룹으로 관리하고 권한을 부여합니다.
* **🤖 AI 이동 비서**: OpenAI(GPT-4o)를 활용하여 가족의 스케줄과 상황에 맞는 이동 의사결정을 지원합니다.
* **📍 실시간 위치 추적**: WebSocket(STOMP)을 이용하여 이동 중인 차량의 실시간 위치를 지도상에 표시합니다.
* **🚕 스마트 카풀 매칭**: Tmap API를 활용하여 경유지를 포함한 최적의 이동 시간과 경로를 계산하고 카풀을 매칭합니다.
* **🎙️ 음성 인식(STT)**: Google Cloud Speech-to-Text를 통해 음성 명령으로 서비스를 제어할 수 있습니다.
* **🌤️ 날씨 정보 연동**: 이동 경로 및 목적지의 실시간 기상 정보를 제공합니다.

## 🛠️ Tech Stack

### Backend
| Category | Technology |
| --- | --- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3.1 |
| **Database** | PostgreSQL, Redis (Caching/Session) |
| **ORM** | JPA (Hibernate) |
| **Security** | Spring Security, JWT |
| **Build Tool** | Gradle |

### Infrastructure
* **Cloud**: AWS EC2, S3, RDS
* **CI/CD**: AWS CodeDeploy, GitHub Actions

### External APIs
* **Map/Mobility**: Tmap API (Geocoding, Route Optimization, Transit)
* **AI/ML**: OpenAI API (GPT-4o), Google Cloud STT, Custom Embedding API
* **Weather**: OpenWeatherMap API

## 🏛️ System Architecture
<img width="900" alt="오카가카 (3)" src="https://github.com/user-attachments/assets/ccfde780-94be-46f7-a736-297c57c152bf" />

## 🚀 Getting Started

### Prerequisites
* Java 17+
* PostgreSQL
* Redis

### Environment Variables (.env)
프로젝트 실행을 위해 다음 환경 변수 설정이 필요합니다.

```bash
# Database
RDS_HOST=localhost
RDS_PORT=5432
RDS_NAME=okagaka_db
RDS_USER=postgres
RDS_PASSWORD=your_password

# AWS
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
S3_BUCKET=your_bucket_name

# External APIs
TMAP_KEY=your_tmap_api_key
OPENAI_API_KEY=your_openai_key
GOOGLE_APPLICATION_CREDENTIALS=path/to/google_credential.json
OPENWEATHERMAP_API_KEY=your_weather_key

# Security
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=3600000

# Client
FRONTEND_ORIGIN=http://localhost:3000
```

### Installation & Run
```bash
# 1. Clone the repository
git clone [https://github.com/your-username/okagaka-backend.git](https://github.com/your-username/okagaka-backend.git)

# 2. Build the project
./gradlew clean build

# 3. Run the application
java -jar build/libs/okagaka-0.0.1-SNAPSHOT.jar
```
