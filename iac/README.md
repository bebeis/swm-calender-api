# IaC (Infrastructure as Code)

SWM Teams(`swm-teams`) 프로젝트의 AWS 인프라를 Terraform으로 관리합니다.

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
│   └── terraform.tfvars.example # 환경별 예시 변수값
└── prod/                # 운영 환경
    ├── main.tf
    ├── variables.tf
    └── terraform.tfvars.example
```

## 아키텍처

각 환경(dev/prod)은 동일한 모듈(`modules/infra`)을 호출하며, 독립된 AWS 리소스를 생성합니다. 모듈 내부의 리전 종속값, 네트워크 CIDR, AZ, EC2 AMI/스펙, RDS 스펙, SSH 키 경로, 공통 태그 등은 입력 변수로 분리되어 있어 공개 저장소에서도 환경별로 안전하게 재사용할 수 있습니다.

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

### 주요 커스터마이징 변수

| 분류 | 변수 | 기본값 |
|------|------|--------|
| 네트워크 | `vpc_cidr_block` | `10.0.0.0/16` |
| 네트워크 | `public_subnet_cidrs` | `10.0.3.0/24`, `10.0.4.0/24` |
| 네트워크 | `private_subnet_cidrs` | `10.0.1.0/24`, `10.0.2.0/24` |
| 네트워크 | `availability_zones` | provider region 기준 자동 선택 |
| EC2 | `ami_id` | 환경에서 직접 지정 |
| EC2 | `instance_type` | 환경에서 직접 지정 |
| EC2 | `ec2_ingress_rules` | 22, 3000, 8080, 9090 포트 허용 |
| RDS | `db_instance_class` | `db.t3.micro` |
| RDS | `db_allocated_storage` | `20` |
| 공통 | `project_name`, `env`, `tags` | 환경에서 직접 지정 |

## 환경별 차이

| 항목 | dev | prod |
|------|-----|------|
| EC2 인스턴스 타입 | t3.micro | t3.small |
| 리소스 이름 접두사 | swm-teams-dev | swm-teams |
| DB 이름 | swm_teams_dev | swm_teams |
| DB 사용자 | swm_teams_dev_user | swm_teams_user |
| Key Pair | swm-teams-dev-key | swm-teams-key |

## 사용법

### 사전 준비

1. [Terraform 설치](https://developer.hashicorp.com/terraform/install) (AWS provider ~> 5.0)
2. AWS CLI 설정 (`aws configure`)
3. SSH 키 페어 생성:

```bash
# dev
cd iac/dev
ssh-keygen -t rsa -b 4096 -f swm-teams-dev-key -N ""

# prod
cd iac/prod
ssh-keygen -t rsa -b 4096 -f swm-teams-key -N ""
```

4. 예시 변수 파일을 복사해서 실제 변수 파일 생성 후 값을 채웁니다.

```bash
# dev
cd iac/dev
cp terraform.tfvars.example terraform.tfvars

# prod
cd ../prod
cp terraform.tfvars.example terraform.tfvars
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

- 실제 비밀번호는 `terraform.tfvars.example`가 아니라 로컬 `terraform.tfvars` 또는 `TF_VAR_db_password` 환경변수로만 관리하세요.
- SSH 키 파일(`.pem`, `.pub`)은 `.gitignore`로 제외되어 있으니 Git에 커밋하지 마세요.
- State 파일(`terraform.tfstate`)은 로컬에 저장됩니다. 팀 협업 시 S3 backend 도입을 권장합니다.
