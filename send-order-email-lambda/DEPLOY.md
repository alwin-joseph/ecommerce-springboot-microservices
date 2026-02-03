# Lambda in Python - Deployment Guide

## Overview

This guide covers deploying the Lambda function for the Send Order Email .

---

## Quick Deployment

### Method 1: AWS Console

#### Step 1: Create Lambda Function

1. Go to **AWS Lambda Console**
2. Click **Create function**
3. Choose **Author from scratch**
4. Configuration:
   - **Function name**: `send-order-email`
   - **Runtime**: `Python 3.11`
   - **Architecture**: `x86_64` or `arm64`
5. Click **Create function**

---

#### Step 2: Upload Code

**Option A: Inline Editor (Recommended)**

1. In function page, scroll to **Code source**
2. Click on `lambda_function.py` in file browser
3. Delete existing code
4. Copy entire contents of your `lambda_function.py`
5. Paste into editor
6. Click **Deploy**
7. Done! ✅

---

#### Step 3: Configure Environment Variables

1. Click **Configuration** tab
2. Click **Environment variables**
3. Click **Edit**
4. Add variables:
   - **Key**: `SENDER_EMAIL`, **Value**: `orders@yourdomain.com`
   - **Key**: `REPLY_TO_EMAIL`, **Value**: `support@yourdomain.com` (optional)
5. Click **Save**

---

#### Step 4: Configure IAM Permissions

1. Click **Configuration** → **Permissions**
2. Click the **Execution role** link
3. Click **Add permissions** → **Attach policies**
4. Attach: `AmazonSESFullAccess`
5. Click **Attach policy**

---

#### Step 6: Test

1. Click **Test** tab
2. Create new test event:
```json
{
  "orderId": "python-test-123",
  "customerEmail": "test@gmail.com",
  "customerName": "Python Test User",
  "productName": "Test Product from Python Lambda",
  "productDescription": "Sent from Python!",
  "quantity": 2,
  "unitPrice": 99.99,
  "totalPrice": 199.98,
  "orderStatus": "CONFIRMED",
  "orderDate": "2026-01-31T10:30:00"
}
```
3. Click **Test**

**Expected Response:**
```json
{
  "statusCode": 200,
  "body": "{\"message\": \"Email sent successfully\", \"messageId\": \"...\", \"orderId\": \"python-test-123\"}"
}
```

4. Check email inbox!

---
