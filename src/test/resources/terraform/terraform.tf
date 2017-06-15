variable publickey {
  description = "SSH Public key used to access nginx EC2 Server"
}

provider "aws" { 
  region = "eu-west-1"
}

/* security group */
resource "aws_security_group" "vm-sg" {
  name        = "gStack-dev-sg"
  description = "Allow ssh inbound traffic and all outbund traffic"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "TCP"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags          = { 
    Project = "gStack"   
    Instance = "gStack-dev"
    Name = "gStack-dev-sg"
  }
}
/* key pair*/
resource "aws_key_pair" "vm-keypair" {
  key_name   = "gStack-dev-key"
  public_key = "${var.publickey}"
}

/* search ami id */
data "aws_ami" "ami" {
  most_recent = true
  filter {
    name   = "name"
    values = ["amzn-ami-hvm-*-x86_64-gp2"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  owners = ["amazon", "309956199498"]
}

/* instance */
resource "aws_instance" "vm" {
  ami           = "${data.aws_ami.ami.id}"
  instance_type = "t2.micro"
  key_name    	= "gStack-dev-key"

  vpc_security_group_ids = [ "${aws_security_group.vm-sg.id}" ]

  user_data     = <<-EOF
              #!/bin/bash
              yum update -y
              EOF

  tags          = { 
    Project = "gStack"
    Instance = "gStack-dev"
    Name = "gStack-dev"
  }
}
