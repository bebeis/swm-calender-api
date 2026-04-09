terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

module "infra" {
  source          = "../modules/infra"
  env             = "dev"
  instance_type   = "t3.micro"
  db_password     = var.db_password
  public_key_path = "${path.module}/swm-calender-dev-key.pub"
}

output "public_ip" {
  value = module.infra.public_ip
}

output "ssh_command" {
  value = module.infra.ssh_command
}

output "instance_id" {
  value = module.infra.instance_id
}

output "ecr_repository_url" {
  value = module.infra.ecr_repository_url
}

output "rds_endpoint" {
  value = module.infra.rds_endpoint
}
