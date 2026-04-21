variable "project_name" {
  description = "Project name used as the base for resource names"
  type        = string
}

variable "env" {
  description = "Environment name (for example dev, staging, prod)"
  type        = string
}

variable "name_prefix" {
  description = "Optional full prefix override for resource names"
  type        = string
  default     = null
}

variable "vpc_cidr_block" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.3.0/24", "10.0.4.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) > 0
    error_message = "public_subnet_cidrs must contain at least one subnet."
  }
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]

  validation {
    condition     = length(var.private_subnet_cidrs) >= 2
    error_message = "private_subnet_cidrs must contain at least two subnets for RDS."
  }
}

variable "availability_zones" {
  description = "Availability zones mapped to subnet CIDRs in order. Leave empty to auto-select from the provider region."
  type        = list(string)
  default     = []
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "ami_id" {
  description = "AMI ID used for the application EC2 instance"
  type        = string
}

variable "enable_detailed_monitoring" {
  description = "Whether to enable EC2 detailed monitoring"
  type        = bool
  default     = true
}

variable "ec2_root_volume_size" {
  description = "EC2 root volume size in GiB"
  type        = number
  default     = 30
}

variable "ec2_root_volume_type" {
  description = "EC2 root volume type"
  type        = string
  default     = "gp3"
}

variable "ec2_ingress_rules" {
  description = "Ingress rules for the EC2 security group"
  type = list(object({
    from_port   = number
    to_port     = number
    protocol    = string
    cidr_blocks = list(string)
    description = string
  }))
  default = [
    {
      from_port   = 8080
      to_port     = 8080
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Allow Spring Boot application"
    },
    {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Allow SSH"
    },
    {
      from_port   = 9090
      to_port     = 9090
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Allow Prometheus"
    },
    {
      from_port   = 3000
      to_port     = 3000
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
      description = "Allow Grafana"
    }
  ]
}

variable "public_key_path" {
  description = "Path to the SSH public key file"
  type        = string
}

variable "ssh_private_key_path" {
  description = "Optional SSH private key path used only for the ssh_command output"
  type        = string
  default     = null
}

variable "docker_compose_version" {
  description = "Docker Compose version installed in user_data"
  type        = string
  default     = "2.20.0"
}

variable "ecr_repository_name" {
  description = "Optional ECR repository name override"
  type        = string
  default     = null
}

variable "ecr_force_delete" {
  description = "Whether to force delete the ECR repository even when it contains images"
  type        = bool
  default     = true
}

variable "ecr_scan_on_push" {
  description = "Whether to scan ECR images on push"
  type        = bool
  default     = true
}

variable "db_name" {
  description = "Optional database name override"
  type        = string
  default     = null
}

variable "db_username" {
  description = "Optional database username override"
  type        = string
  default     = null
}

variable "db_password" {
  description = "RDS database password"
  type        = string
  sensitive   = true
}

variable "db_port" {
  description = "Database port"
  type        = number
  default     = 3306
}

variable "db_engine" {
  description = "RDS engine"
  type        = string
  default     = "mysql"
}

variable "db_engine_version" {
  description = "RDS engine version"
  type        = string
  default     = "8.0"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Allocated RDS storage in GiB"
  type        = number
  default     = 20
}

variable "db_storage_type" {
  description = "RDS storage type"
  type        = string
  default     = "gp2"
}

variable "db_skip_final_snapshot" {
  description = "Whether to skip the final snapshot when destroying the DB"
  type        = bool
  default     = true
}

variable "db_publicly_accessible" {
  description = "Whether the database should be publicly accessible"
  type        = bool
  default     = false
}

variable "db_monitoring_interval" {
  description = "RDS enhanced monitoring interval in seconds"
  type        = number
  default     = 60
}

variable "tags" {
  description = "Additional tags applied to supported resources"
  type        = map(string)
  default     = {}
}
