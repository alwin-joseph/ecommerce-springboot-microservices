"""
AWS Lambda Function: Send Order Confirmation Email (Python)

This Lambda function receives order events from the Order Service
and sends confirmation emails via Amazon SES.

Environment Variables:
- SENDER_EMAIL: Email address to send from (must be verified in SES)
- REPLY_TO_EMAIL: Optional reply-to email address
- AWS_REGION: AWS region (default: us-east-1)

"""

import json
import os
import logging
from datetime import datetime
from decimal import Decimal
import boto3
from botocore.exceptions import ClientError

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize SES client (reused across invocations for performance)
ses_client = boto3.client('ses', region_name=os.environ.get('AWS_REGION', 'ap-south-1'))

# Configuration from environment variables
SENDER_EMAIL = os.environ.get('SENDER_EMAIL')
REPLY_TO_EMAIL = os.environ.get('REPLY_TO_EMAIL', '')


def lambda_handler(event, context):
    """
    Lambda Handler Function
    
    This is the entry point for the Lambda function.
    It receives the order event, formats the email, and sends it via SES.
    
    Args:
        event (dict): Order email event from Java service
        context (object): Lambda context with runtime information
        
    Returns:
        dict: Response with statusCode and message
    """
    
    logger.info('Lambda function invoked')
    # logger.info(f'Request ID: {context.request_id}')
    logger.info(f'Function Name: {context.function_name}')
    logger.info(f'Event received: {json.dumps(event, default=str)}')
    
    try:
        # Validate required fields
        validate_event(event)
        
        # Extract order data
        order_id = event.get('orderId')
        customer_name = event.get('customerName')
        
        logger.info(f'Processing order: {order_id} for customer: {customer_name}')
        
        # Send email via SES
        message_id = send_order_confirmation_email(event)
        
        logger.info(f'Email sent successfully. Message ID: {message_id}')
        
        # Return success response
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Email sent successfully',
                'messageId': message_id,
                'orderId': order_id
            })
        }
        
    except ValueError as e:
        # Validation error
        logger.error(f'Validation error: {str(e)}')
        return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'Validation error',
                'message': str(e),
                'orderId': event.get('orderId', 'unknown')
            })
        }
        
    except ClientError as e:
        # AWS SES error
        logger.error(f'SES error: {e.response["Error"]["Message"]}')
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'SES error',
                'message': e.response['Error']['Message'],
                'orderId': event.get('orderId', 'unknown')
            })
        }
        
    except Exception as e:
        # General error
        logger.error(f'Unexpected error: {str(e)}', exc_info=True)
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'Internal server error',
                'message': str(e),
                'orderId': event.get('orderId', 'unknown')
            })
        }


def validate_event(event):
    """
    Validate Order Email Event
    
    Ensures all required fields are present.
    
    Args:
        event (dict): Order email event
        
    Raises:
        ValueError: If validation fails
    """
    required_fields = ['orderId', 'customerEmail', 'customerName', 'productName']
    
    for field in required_fields:
        if not event.get(field):
            raise ValueError(f'{field} is required')
    
    # Validate SENDER_EMAIL is configured
    if not SENDER_EMAIL:
        raise ValueError('SENDER_EMAIL environment variable is required')


def send_order_confirmation_email(event):
    """
    Send Order Confirmation Email via SES
    
    Creates and sends both HTML and plain text versions of the email.
    
    Args:
        event (dict): Order email event
        
    Returns:
        str: SES message ID
        
    Raises:
        ClientError: If SES sending fails
    """
    
    logger.info('Generating email content...')
    
    # Generate email content
    html_body = generate_html_email(event)
    text_body = generate_text_email(event)
    
    logger.info('Email content generated successfully')
    
    # Create email subject
    subject = f"Order Confirmation - Order #{event.get('orderId')}"
    
    # Build email parameters
    email_params = {
        'Source': SENDER_EMAIL,
        'Destination': {
            'ToAddresses': [event.get('customerEmail')]
        },
        'Message': {
            'Subject': {
                'Data': subject,
                'Charset': 'UTF-8'
            },
            'Body': {
                'Html': {
                    'Data': html_body,
                    'Charset': 'UTF-8'
                },
                'Text': {
                    'Data': text_body,
                    'Charset': 'UTF-8'
                }
            }
        }
    }
    
    # Add reply-to if configured
    if REPLY_TO_EMAIL:
        email_params['ReplyToAddresses'] = [REPLY_TO_EMAIL]
    
    logger.info(f'Sending email to: {event.get("customerEmail")}')
    
    # Send email
    response = ses_client.send_email(**email_params)
    
    return response['MessageId']


def generate_html_email(event):
    """
    Generate HTML Email Template
    
    Creates a beautiful HTML email with order details.
    
    Args:
        event (dict): Order email event
        
    Returns:
        str: HTML email content
    """
    
    # Extract data with defaults
    order_id = event.get('orderId', 'N/A')
    customer_name = escape_html(event.get('customerName', 'Customer'))
    customer_email = escape_html(event.get('customerEmail', ''))
    product_name = escape_html(event.get('productName', 'Product'))
    product_description = escape_html(event.get('productDescription', ''))
    quantity = event.get('quantity', 1)
    unit_price = event.get('unitPrice')
    total_price = event.get('totalPrice', 0)
    order_status = event.get('orderStatus', 'CONFIRMED')
    order_date = format_date(event.get('orderDate'))
    
    # Build HTML
    html = f'''
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Confirmation</title>
    <style>
        {get_email_styles()}
    </style>
</head>
<body>
    <div class="container">
        <!-- Header -->
        <div class="header">
            <h1>✅ Order Confirmed!</h1>
            <p>Thank you for your purchase</p>
        </div>
        
        <!-- Content -->
        <div class="content">
            <div class="greeting">
                Hi {customer_name},
            </div>
            
            <p>
                We're excited to confirm that your order has been received and is being processed.
                Here are the details of your purchase:
            </p>
            
            <!-- Order Details -->
            <div class="order-details">
                <h2>📦 Order Details</h2>
                
                <div class="detail-row">
                    <span class="detail-label">Order ID:</span>
                    <span class="detail-value">#{order_id}</span>
                </div>
                
                <div class="detail-row">
                    <span class="detail-label">Order Date:</span>
                    <span class="detail-value">{order_date}</span>
                </div>
                
                <div class="detail-row">
                    <span class="detail-label">Status:</span>
                    <span class="status-badge">{order_status}</span>
                </div>
                
                <div class="detail-row">
                    <span class="detail-label">Product:</span>
                    <span class="detail-value">{product_name}</span>
                </div>
                
                {f'<div class="detail-row"><span class="detail-label">Description:</span><span class="detail-value">{product_description}</span></div>' if product_description else ''}
                
                <div class="detail-row">
                    <span class="detail-label">Quantity:</span>
                    <span class="detail-value">{quantity}</span>
                </div>
                
                {f'<div class="detail-row"><span class="detail-label">Unit Price:</span><span class="detail-value">${format_price(unit_price)}</span></div>' if unit_price else ''}
                
                <div class="detail-row">
                    <span class="detail-label">Total Amount:</span>
                    <span class="total-price">${format_price(total_price)}</span>
                </div>
            </div>
            
            <!-- Next Steps -->
            <div class="message">
                <strong>📬 What's Next?</strong><br>
                We'll send you another email with tracking information once your order ships.
                Expected delivery: 3-5 business days.
            </div>
            
            <center>
                <a href="https://yourdomain.com/orders/{order_id}" class="button">
                    Track Your Order
                </a>
            </center>
            
            <p>
                If you have any questions about your order, please don't hesitate to contact our customer support team.
            </p>
        </div>
        
        <!-- Footer -->
        <div class="footer">
            <p>
                Need help? <a href="mailto:support@yourdomain.com">Contact Support</a>
            </p>
            <p>
                © 2024 Your Company. All rights reserved.
            </p>
            <p style="font-size: 12px; color: #adb5bd; margin-top: 10px;">
                This email was sent to {customer_email} because you placed an order.
            </p>
        </div>
    </div>
</body>
</html>
'''
    
    return html


def generate_text_email(event):
    """
    Generate Plain Text Email Template
    
    Creates a plain text version for non-HTML email clients.
    
    Args:
        event (dict): Order email event
        
    Returns:
        str: Plain text email content
    """
    
    # Extract data
    order_id = event.get('orderId', 'N/A')
    customer_name = event.get('customerName', 'Customer')
    customer_email = event.get('customerEmail', '')
    product_name = event.get('productName', 'Product')
    product_description = event.get('productDescription', '')
    quantity = event.get('quantity', 1)
    unit_price = event.get('unitPrice')
    total_price = event.get('totalPrice', 0)
    order_status = event.get('orderStatus', 'CONFIRMED')
    order_date = format_date(event.get('orderDate'))
    
    # Build text
    text = f'''
ORDER CONFIRMATION
==================

Hi {customer_name},

Thank you for your order! We're excited to confirm that your order has been received and is being processed.

ORDER DETAILS:
--------------
Order ID: #{order_id}
Order Date: {order_date}
Status: {order_status}

PRODUCT DETAILS:
----------------
Product: {product_name}
{f'Description: {product_description}' if product_description else ''}
Quantity: {quantity}
{f'Unit Price: ${format_price(unit_price)}' if unit_price else ''}
Total Amount: ${format_price(total_price)}

WHAT'S NEXT?
------------
We'll send you another email with tracking information once your order ships.
Expected delivery: 3-5 business days.

Track your order: https://yourdomain.com/orders/{order_id}

NEED HELP?
----------
If you have any questions about your order, please contact our customer support team at support@yourdomain.com

© 2024 Your Company. All rights reserved.

This email was sent to {customer_email} because you placed an order.
'''
    
    return text


def get_email_styles():
    """
    Get Email CSS Styles
    
    Returns:
        str: CSS styles for HTML email
    """
    return '''
        body {
            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
        }
        .container {
            max-width: 600px;
            margin: 20px auto;
            background: #ffffff;
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 28px;
            font-weight: 600;
        }
        .header p {
            margin: 10px 0 0 0;
            font-size: 16px;
            opacity: 0.9;
        }
        .content {
            padding: 30px 20px;
        }
        .greeting {
            font-size: 18px;
            color: #333;
            margin-bottom: 20px;
        }
        .order-details {
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            padding: 20px;
            margin: 20px 0;
            border-radius: 5px;
        }
        .order-details h2 {
            margin-top: 0;
            color: #667eea;
            font-size: 20px;
        }
        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #e9ecef;
        }
        .detail-row:last-child {
            border-bottom: none;
        }
        .detail-label {
            font-weight: 600;
            color: #495057;
        }
        .detail-value {
            color: #212529;
        }
        .total-price {
            font-size: 24px;
            font-weight: bold;
            color: #667eea;
        }
        .status-badge {
            display: inline-block;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
            background-color: #28a745;
            color: white;
        }
        .message {
            background: #e7f3ff;
            border-left: 4px solid #007bff;
            padding: 15px;
            margin: 20px 0;
            border-radius: 5px;
        }
        .footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            font-size: 14px;
        }
        .footer a {
            color: #667eea;
            text-decoration: none;
        }
        .button {
            display: inline-block;
            padding: 12px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 5px;
            margin: 20px 0;
            font-weight: 600;
        }
    '''


def format_date(date_string):
    """
    Format date for display
    
    Args:
        date_string (str): ISO format date string
        
    Returns:
        str: Formatted date string
    """
    if not date_string:
        return 'N/A'
    
    try:
        # Parse ISO format (e.g., "2024-01-31T10:30:00")
        dt = datetime.fromisoformat(date_string.replace('Z', '+00:00'))
        return dt.strftime('%B %d, %Y at %I:%M %p')
    except Exception as e:
        logger.warning(f'Failed to parse date: {date_string}, error: {e}')
        return str(date_string)


def format_price(price):
    """
    Format price with 2 decimal places
    
    Args:
        price (float/Decimal/str): Price value
        
    Returns:
        str: Formatted price (e.g., "99.99")
    """
    if price is None:
        return '0.00'
    
    try:
        if isinstance(price, str):
            price = float(price)
        return f'{float(price):.2f}'
    except Exception:
        return '0.00'


def escape_html(text):
    """
    Escape HTML special characters
    
    Args:
        text (str): Text to escape
        
    Returns:
        str: HTML-safe text
    """
    if not text:
        return ''
    
    return (str(text)
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;')
            .replace("'", '&#39;'))


# For local testing
if __name__ == '__main__':
    # Set environment variable
    os.environ['SENDER_EMAIL'] = 'test@example.com'
    
    # Create test event
    test_event = {
        'orderId': 'test-123',
        'customerEmail': 'customer@example.com',
        'customerName': 'Test User',
        'productName': 'Test Product',
        'productDescription': 'This is a test product',
        'quantity': 2,
        'unitPrice': 99.99,
        'totalPrice': 199.98,
        'orderStatus': 'CONFIRMED',
        'orderDate': '2024-01-31T10:30:00'
    }
    
    # Create mock context
    class MockContext:
        request_id = 'test-request-id'
        function_name = 'send-order-email'
    
    # Test handler
    result = lambda_handler(test_event, MockContext())
    print(json.dumps(result, indent=2))
