# 🪣 Spring Boot × Amazon S3

A Spring Boot REST API for managing files in AWS S3 with support for:

 - 📤 Upload files to category folders
 - 📂 List files by folder
 - 📥 Download files
 - 🗄️ Soft delete (archive instead of permanent delete)
 - 🔐 Pre-signed URLs for secure access

---

## 🧠 Project Overview

This project demonstrates a real-world backend system using:

 - Spring Boot
 - AWS S3 (SDK v2)
 - Clean layered architecture (Controller → Service → Config)

Instead of deleting files permanently, this system **archives them with timestamps**, making it safer and audit-friendly.

---

## 📁 S3 Bucket Structure

```
s3-bucket-name/                    ← s3 bucket name
│── Pictures/
│    ├── Beaches/
│    │   └── IMG_1.JPG
│    └── Waterfalls/
│       ├── IMG_2.JPG
│       └── IMG_3.JPG
└── Archive/                        ← soft-deleted files land here
     └── Waterfalls_IMG_4.JPG_20260409T191501
```

> All "folders" in S3 are virtual — they are key prefixes that end with `/`.

---

## 🏗️ Project Structure

```
src/
├── main/java/com/bhuvan/s3/
│   ├── S3Application.java              # Entry point
│   ├── config/
│   │   ├── S3Config.java               # S3Client + S3Presigner beans
│   │   └── OpenApiConfig.java          # Swagger config
│   ├── controller/
│   │   └── S3Controller.java           # REST endpoints
│   ├── service/
│   │   └── S3Service.java              # Upload / List / Download / Archive / Delete
│   ├── model/
│   │   └── S3Response.java             # Response DTOs
│   └── exception/
│       ├── S3Exception.java            # Domain exceptions
│       └── GlobalExceptionHandler.java # @RestControllerAdvice
├── main/resources/
│    └── application.yml                # Configuration (reads env vars)
├── README.md                           # you are here
└── API_REFERENCE.md                    # Contains api's responses   
```

---

## 🛠️ Tech Stack

| Layer                 | Technology                    |
|-----------------------|-------------------------------|
| Language              | Java 17                       |
| Framework             | Spring Boot 3.2.5             |
| AWS SDK               | AWS SDK for Java v2 (2.29.52) |
| Build                 | Maven                         |
| Boilerplate reduction | Lombok                        |
| Tests                 | JUnit 5 + Mockito             |
 
---

## 🚀 Quick Start

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- An AWS account with an S3 bucket
- IAM user with `AmazonS3FullAccess` (or a scoped custom policy — see below)

## 🔒 IAM Policy

Attach this policy to your IAM user to grant only the permissions this app needs:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:CopyObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::YOUR-S3-BUCKET-NAME"
      ]
    }
  ]
}
```

### 2. Clone the Repo

```bash
git clone https://github.com/bhuvan-tej/s3-spring-boot.git
cd s3-spring-boot
```

### 3. Set AWS Credentials (Environment Variables)

**Never put credentials in application.yml or commit them to git.**

```bash
# Linux / macOS
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
export AWS_REGION=your-region
export AWS_S3_BUCKET=your-s3-bucket-name
 
# Windows PowerShell
$env:AWS_ACCESS_KEY_ID="your-access-key-id"
$env:AWS_SECRET_ACCESS_KEY="your-secret-access-key"
$env:AWS_REGION="your-region"
$env:AWS_S3_BUCKET="your-s3-bucket-name"
```

Alternatively, configure `~/.aws/credentials` via the AWS CLI:
```bash
aws configure
```

### 4. Build & Run

```bash
# Build
mvn clean package
 
# Run
mvn spring-boot:run
```

The server starts at **http://localhost:8080**
 
---