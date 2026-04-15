# 📡 API Reference
Base URL: `http://localhost:8080/api/v1/s3`

## 1. Upload a File

---
```
POST /api/s3/upload/{folder}
Content-Type: multipart/form-data
```

| Parameter | Type      | Description                                      |
|-----------|-----------|--------------------------------------------------|
| `folder`  | path      | Target subfolder (e.g., `Beaches`, `Waterfalls`) |
| `file`    | form-data | The image file to upload                         |

**CURL example:**
```bash
curl -X 'POST' \
  'http://localhost:8080/api/s3/upload/Beaches' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@ar_pr.jpg;type=image/jpeg'
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "File uploaded successfully.",
  "key": "Pictures/Beaches/cluster.png"
}
```

##  2. List Files in a Folder

---

```
GET /api/s3/files?folder={folder}
```

**CURL example:**
```bash
curl -X 'GET' \
  'http://localhost:8080/api/s3/files?folder=Beaches' \
  -H 'accept: application/json'
```

**Response (200 OK):**
```json
{
  "folder": "Waterfalls",
  "prefix": "Pictures/Waterfalls/",
  "files": [
    {
      "key": "IMG_2.jpg",
      "fileName": ".jpg",
      "size": 136283,
      "lastModified": "2026-04-12T11:42:38Z",
      "storageClass": "STANDARD"
    },
    {
      "key": "IMG_3",
      "fileName": "cluster.png",
      "size": 679874,
      "lastModified": "2026-04-11T08:50:50Z",
      "storageClass": "STANDARD"
    }
  ],
  "count": 2
}
```

## 3. Download a File (Pre-signed URL)

---

```
GET /api/v1/s3/{folder}/files/{fileName}
```

Returns a **15-minute** pre-signed URL. No AWS credentials required to use it.

```bash
curl http://localhost:8080/api/v1/s3/Waterfalls/files/IMG_2.JPG
```

**Response `200 OK`**
```json
{
  "fileName": "IMG_2.JPG",
  "key": "Pictures/Waterfalls/IMG_4694.JPG",
  "presignedUrl": "https://bucket-name.amazonaws.com/Pictures/Waterfalls/IMG_2.JPG?X-Amz-...",
  "expiresInMinutes": 15
}
```

## 4. Archive a File (Soft Delete) ⭐

---

```
POST /api/v1/s3/{folder}/files/{fileName}/archive
```

Moves the file to `Archive/{folder}_{name}_{timestamp}.{ext}` then deletes the original.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/s3/Beaches/files/IMG_1.jpg/archive' \
  -H 'accept: */*' \
  -d ''
```

**Response `200 OK`**
```json
{
  "originalKey": "Pictures/Beaches/IMG_1.jpg",
  "archivedKey": "Archive/IMG_1.jpg_20260409T230624",
  "message": "File archived successfully. To permanently delete, remove from Archive folder."
}
```

## 5. Hard Delete (Permanent)

---

```
DELETE /api/v1/s3/{folder}/files/{fileName}
```
 
> ⚠️ Irreversible — prefer Archive for recoverable deletes.
 
```bash
curl -X DELETE http://localhost:8080/api/v1/s3/Beaches/files/IMG_1.jpg
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "File permanently deleted.",
  "key": "Pictures/Beaches/IMG_1.jpg"
}
```

## ❌ Error Responses

---

All errors follow the same shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "File not found in S3: Pictures/Beaches/missing.PNG",
  "timestamp": "2026-04-09T19:16:15Z"
}
```

| Status | Scenario                              |
|--------|---------------------------------------|
| 400    | Blank folder / empty file             |
| 404    | Object key does not exist             |
| 413    | File exceeds 50 MB limit              |
| 500    | AWS SDK error or unexpected exception |

## 🖥️ Swagger UI

---

Once running, open **http://localhost:8080/swagger-ui.html** to explore and test all endpoints interactively.

| URL                                     | Description            |
|-----------------------------------------|------------------------|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI |
| `http://localhost:8080/v3/api-docs`     | Raw OpenAPI 3.0 JSON   |

You can test every endpoint directly in the browser — upload files, list with pagination, generate download URLs, and archive files — all without Postman.