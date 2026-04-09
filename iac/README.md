# IaC (Infrastructure as Code)

SWM Calendar Sync 프로젝트의 AWS 인프라를 Terraform으로 관리합니다.

## 디렉토리 구조

```
iac/
├── modules/infra/       # 공통 인프라 모듈
│   ├── main.tf          # 리소스 정의
│   ├── variables.tf     # 모듈 입력 변수
│   └── outputs.tf       # 모듈 출력값
├── dev/                 # 개발 환경
│   ├── main.tf          # 모듈 호출 + provider 설정
│   ├── variables.tf     # 환경 변수 선언
│   └── terraform.tfvars # 환경별 변수값
└── prod/                # 운영 환경
    ├── main.tf
    ├── variables.tf
    └── terraform.tfvars
```

## 아키텍처

각 환경(dev/prod)은 동일한 모듈(`modules/infra`)을 호출하며, 독립된 AWS 리소스를 생성합니다.

```
VPC (10.0.0.0/16)
├── Public Subnet 1  (10.0.3.0/24, ap-northeast-2a) ── EC2
├── Public Subnet 2  (10.0.4.0/24, ap-northeast-2c)
├── Private Subnet 1 (10.0.1.0/24, ap-northeast-2a) ── RDS
└── Private Subnet 2 (10.0.2.0/24, ap-northeast-2c) ── RDS
```

### 생성되는 리소스

| 카테고리 | 리소스 | 설명 |
|----------|--------|------|
| 네트워크 | VPC, Subnets, IGW, Route Table | Public/Private 서브넷 분리 |
| 컴퓨팅 | EC2 + EIP | Spring Boot 애플리케이션 서버 |
| 데이터베이스 | RDS (MySQL 8.0) | Private 서브넷, EC2에서만 접근 가능 |
| 컨테이너 | ECR | Docker 이미지 저장소 |
| 모니터링 | CloudWatch Dashboard, Enhanced Monitoring | EC2/RDS CPU, 메모리, 연결 수 등 |
| 보안 | Security Groups, IAM Roles | EC2→RDS 접근 제어, SSM/ECR/CloudWatch 권한 |

## 환경별 차이

| 항목 | dev | prod |
|------|-----|------|
| EC2 인스턴스 타입 | t3.micro | t3.small |
| 리소스 이름 접두사 | swm-calender-dev | swm-calender |
| DB 이름 | swm_calender_dev | swm_calender |
| DB 사용자 | swm_calender_dev_user | swm_calender_user |
| Key Pair | swm-calender-dev-key | swm-calender-key |

## 사용법

### 사전 준비

1. [Terraform 설치](https://developer.hashicorp.com/terraform/install) (AWS provider ~> 5.0)
2. AWS CLI 설정 (`aws configure`)
3. SSH 키 페어 생성:

```bash
# dev
cd iac/dev
ssh-keygen -t rsa -b 4096 -f swm-calender-dev-key -N ""

# prod
cd iac/prod
ssh-keygen -t rsa -b 4096 -f swm-calender-key -N ""
```

### 인프라 배포

```bash
# dev 환경
cd iac/dev
terraform init
terraform plan
terraform apply

# prod 환경
cd iac/prod
terraform init
terraform plan
terraform apply
```

### 인프라 삭제

```bash
cd iac/dev   # 또는 iac/prod
terraform destroy
```

## 출력값

`terraform apply` 완료 후 아래 값들이 출력됩니다:

| 출력 | 설명 |
|------|------|
| `public_ip` | EC2 퍼블릭 IP |
| `ssh_command` | SSH 접속 명령어 |
| `instance_id` | EC2 인스턴스 ID |
| `ecr_repository_url` | ECR 저장소 URL |
| `rds_endpoint` | RDS 접속 엔드포인트 |

## 보안 참고

- `terraform.tfvars`에 DB 비밀번호가 포함되어 있으므로 `.gitignore`에 `*.tfvars`를 추가하거나, `TF_VAR_db_password` 환경변수를 사용하는 것을 권장합니다.
- SSH 키 파일(`.pem`, `.pub`)은 절대 Git에 커밋하지 마세요.
- State 파일(`terraform.tfstate`)은 로컬에 저장됩니다. 팀 협업 시 S3 backend 도입을 권장합니다.
