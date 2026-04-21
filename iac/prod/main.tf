terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "infra" {
  source = "../modules/infra"

  project_name         = var.project_name
  env                  = var.env
  instance_type        = var.instance_type
  ami_id               = var.ami_id
  public_key_path      = abspath(var.public_key_path)
  ssh_private_key_path = abspath(var.ssh_private_key_path)
  db_password          = var.db_password

  vpc_cidr_block       = var.vpc_cidr_block
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  db_instance_class    = var.db_instance_class
  tags                 = var.tags
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
