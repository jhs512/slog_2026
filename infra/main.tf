provider "aws" {
  region = var.region
}

# VPC
resource "aws_vpc" "vpc_1" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = {
    Name = "${var.prefix}-vpc-1"
  }
}

# Subnets
resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.prefix}-subnet-1"
  }
}

resource "aws_subnet" "subnet_2" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.prefix}-subnet-2"
  }
}

resource "aws_subnet" "subnet_3" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "${var.region}c"
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.prefix}-subnet-3"
  }
}

resource "aws_subnet" "subnet_4" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "${var.region}d"
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.prefix}-subnet-4"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id
  tags = {
    Name = "${var.prefix}-igw-1"
  }
}

# Route Table
resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }
  tags = {
    Name = "${var.prefix}-rt-1"
  }
}

resource "aws_route_table_association" "rta_1" {
  subnet_id      = aws_subnet.subnet_1.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "rta_2" {
  subnet_id      = aws_subnet.subnet_2.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "rta_3" {
  subnet_id      = aws_subnet.subnet_3.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "rta_4" {
  subnet_id      = aws_subnet.subnet_4.id
  route_table_id = aws_route_table.rt_1.id
}

# Security Group
resource "aws_security_group" "sg_1" {
  vpc_id = aws_vpc.vpc_1.id
  tags = {
    Name = "${var.prefix}-sg-1"
  }
}

resource "aws_vpc_security_group_ingress_rule" "sg_1_ingress_1" {
  security_group_id = aws_security_group.sg_1.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 0
  to_port           = 65535
}

resource "aws_vpc_security_group_egress_rule" "sg_1_egress_1" {
  security_group_id = aws_security_group.sg_1.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# IAM Role
resource "aws_iam_role" "ec2_role_1" {
  name = "${var.prefix}-ec2-role-1"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
  tags = {
    Name = "${var.prefix}-ec2-role-1"
  }
}

resource "aws_iam_role_policy_attachment" "ec2_role_1_s3" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_role_policy_attachment" "ec2_role_1_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachments_exclusive" "ec2_role_1_exclusive" {
  role_name   = aws_iam_role.ec2_role_1.name
  policy_arns = [
    "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
  ]
}

resource "aws_iam_instance_profile" "ec2_profile_1" {
  name = "${var.prefix}-ec2-profile-1"
  role = aws_iam_role.ec2_role_1.name
}

# EC2
data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "ec2_1" {
  ami                    = data.aws_ami.latest_amazon_linux.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.subnet_1.id
  vpc_security_group_ids = [aws_security_group.sg_1.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile_1.name

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
  }

  tags = {
    Name = "${var.prefix}-ec2-1"
  }

  user_data = <<-EOF
    #!/bin/bash

    # Swap 4GB
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo "/swapfile swap swap defaults 0 0" >> /etc/fstab

    # Timezone
    timedatectl set-timezone Asia/Seoul

    # Environment Variables
    echo "export PASSWORD_1=${var.password_1}" >> /root/.bashrc
    echo "export APP_1_DOMAIN=${var.app_1_domain}" >> /root/.bashrc
    echo "export APP_1_DB_NAME=${var.app_1_db_name}" >> /root/.bashrc
    echo "export GITHUB_ACCESS_TOKEN_1_OWNER=${var.github_access_token_1_owner}" >> /root/.bashrc
    echo "export GITHUB_ACCESS_TOKEN_1=${var.github_access_token_1}" >> /root/.bashrc

    export PASSWORD_1=${var.password_1}
    export APP_1_DOMAIN=${var.app_1_domain}
    export APP_1_DB_NAME=${var.app_1_db_name}
    export GITHUB_ACCESS_TOKEN_1_OWNER=${var.github_access_token_1_owner}
    export GITHUB_ACCESS_TOKEN_1=${var.github_access_token_1}

    # Docker
    dnf install -y docker
    systemctl start docker
    systemctl enable docker

    docker network create common

    # Nginx Proxy Manager
    docker run -d \
      --name npm \
      --network common \
      --restart always \
      -p 80:80 \
      -p 443:443 \
      -p 81:81 \
      -v /dockerdata/npm/data:/data \
      -v /dockerdata/npm/letsencrypt:/etc/letsencrypt \
      -e TZ=Asia/Seoul \
      jc21/nginx-proxy-manager:latest

    # Redis
    docker run -d \
      --name redis_1 \
      --network common \
      --restart always \
      -p 6379:6379 \
      -v /dockerdata/redis_1:/data \
      -e TZ=Asia/Seoul \
      redis \
      redis-server --requirepass "$PASSWORD_1" --appendonly yes

    # PostgreSQL
    docker run -d \
      --name postgresql_1 \
      --network common \
      --restart always \
      -p 5432:5432 \
      -v /dockerdata/postgresql_1:/var/lib/postgresql \
      -e POSTGRES_USER=slog \
      -e POSTGRES_PASSWORD="$PASSWORD_1" \
      -e POSTGRES_DB="$APP_1_DB_NAME" \
      -e TZ=Asia/Seoul \
      postgres:18

    # GHCR Login
    echo "$GITHUB_ACCESS_TOKEN_1" | docker login ghcr.io -u "$GITHUB_ACCESS_TOKEN_1_OWNER" --password-stdin
  EOF
}
