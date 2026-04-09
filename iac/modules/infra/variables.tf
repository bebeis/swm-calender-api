variable "env" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "db_password" {
  description = "RDS root user password"
  type        = string
  sensitive   = true
}

variable "public_key_path" {
  description = "Path to SSH public key file"
  type        = string
}
