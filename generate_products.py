#!/usr/bin/env python3
"""
Generate a massive product dataset for testing
Creates JSON and CSV files with thousands of fake products
"""

import json
import csv
import random
from datetime import datetime, timedelta
import uuid

# Product categories and names
CATEGORIES = [
    "Electronics", "Clothing", "Home & Garden", "Sports", "Books", 
    "Beauty", "Toys", "Automotive", "Health", "Food", "Tools", "Music"
]

PRODUCT_NAMES = {
    "Electronics": ["Smartphone", "Laptop", "Tablet", "Headphones", "Camera", "TV", "Speaker", "Monitor"],
    "Clothing": ["T-Shirt", "Jeans", "Dress", "Jacket", "Shoes", "Hat", "Sweater", "Pants"],
    "Home & Garden": ["Chair", "Table", "Lamp", "Pillow", "Curtain", "Rug", "Plant", "Vase"],
    "Sports": ["Soccer Ball", "Tennis Racket", "Yoga Mat", "Dumbbell", "Bicycle", "Running Shoes"],
    "Books": ["Novel", "Cookbook", "Guide", "Biography", "Manual", "Dictionary"],
    "Beauty": ["Foundation", "Lipstick", "Shampoo", "Perfume", "Moisturizer", "Nail Polish"],
    "Toys": ["Action Figure", "Puzzle", "Board Game", "Doll", "Building Blocks", "Car Toy"],
    "Automotive": ["Tire", "Engine Oil", "Car Battery", "Brake Pad", "Air Filter", "Spark Plug"],
    "Health": ["Vitamin", "Protein Powder", "First Aid Kit", "Thermometer", "Blood Pressure Monitor"],
    "Food": ["Pasta", "Rice", "Olive Oil", "Chocolate", "Coffee", "Tea", "Honey", "Bread"],
    "Tools": ["Hammer", "Screwdriver", "Drill", "Saw", "Wrench", "Pliers", "Level"],
    "Music": ["Guitar", "Piano", "Microphone", "Drum Set", "Violin", "Music Stand"]
}

BRANDS = [
    "TechCorp", "StyleMax", "HomeComfort", "SportsPro", "BookWorm", "BeautyGlow",
    "PlayTime", "AutoExpert", "WellnessFirst", "FoodieChoice", "ToolMaster", "SoundWave",
    "EliteGear", "ModernStyle", "ComfortZone", "ActiveLife", "SmartRead", "GlamourLux"
]

ADJECTIVES = [
    "Premium", "Professional", "Deluxe", "Ultra", "Advanced", "Classic", "Modern", 
    "Eco-Friendly", "High-Performance", "Luxury", "Compact", "Wireless", "Smart",
    "Durable", "Lightweight", "Waterproof", "Portable", "Energy-Saving"
]

def generate_product(product_id):
    """Generate a single product with realistic data"""
    category = random.choice(CATEGORIES)
    base_name = random.choice(PRODUCT_NAMES[category])
    brand = random.choice(BRANDS)
    adjective = random.choice(ADJECTIVES)
    
    # Create product name
    name = f"{brand} {adjective} {base_name}"
    
    # Generate realistic price based on category
    price_ranges = {
        "Electronics": (50, 2000),
        "Clothing": (15, 300),
        "Home & Garden": (20, 500),
        "Sports": (25, 800),
        "Books": (10, 50),
        "Beauty": (8, 150),
        "Toys": (12, 200),
        "Automotive": (30, 1500),
        "Health": (15, 300),
        "Food": (3, 100),
        "Tools": (20, 500),
        "Music": (50, 3000)
    }
    
    min_price, max_price = price_ranges[category]
    price = round(random.uniform(min_price, max_price), 2)
    
    # Generate other fields
    rating = round(random.uniform(1.0, 5.0), 1)
    stock = random.randint(0, 500)
    discount = random.choice([0, 5, 10, 15, 20, 25, 30]) if random.random() < 0.3 else 0
    
    # Generate dates
    created_date = datetime.now() - timedelta(days=random.randint(1, 365))
    updated_date = created_date + timedelta(days=random.randint(0, 30))
    
    # Generate description
    descriptions = [
        f"High-quality {base_name.lower()} perfect for daily use.",
        f"Experience the best {base_name.lower()} technology has to offer.",
        f"Premium {base_name.lower()} designed for performance and durability.",
        f"Professional-grade {base_name.lower()} for serious enthusiasts.",
        f"Affordable yet reliable {base_name.lower()} for everyone."
    ]
    
    product = {
        "id": product_id,
        "uuid": str(uuid.uuid4()),
        "name": name,
        "brand": brand,
        "category": category,
        "subcategory": f"{category} > {base_name}",
        "description": random.choice(descriptions),
        "price": price,
        "currency": "USD",
        "discount_percent": discount,
        "final_price": round(price * (1 - discount/100), 2),
        "rating": rating,
        "review_count": random.randint(0, 1000),
        "stock_quantity": stock,
        "in_stock": stock > 0,
        "sku": f"{brand[:3].upper()}-{random.randint(100000, 999999)}",
        "barcode": f"{random.randint(1000000000000, 9999999999999)}",
        "weight_kg": round(random.uniform(0.1, 50.0), 2),
        "dimensions": {
            "length_cm": round(random.uniform(5, 100), 1),
            "width_cm": round(random.uniform(5, 100), 1),
            "height_cm": round(random.uniform(2, 50), 1)
        },
        "tags": [category.lower(), base_name.lower(), brand.lower()],
        "image_url": f"https://picsum.photos/400/400?random={product_id}",
        "thumbnail_url": f"https://picsum.photos/200/200?random={product_id}",
        "created_at": created_date.isoformat(),
        "updated_at": updated_date.isoformat(),
        "status": random.choice(["active", "active", "active", "inactive", "draft"]),
        "featured": random.random() < 0.1,
        "shipping": {
            "free_shipping": random.random() < 0.4,
            "shipping_cost": 0 if random.random() < 0.4 else round(random.uniform(5, 25), 2),
            "estimated_days": random.randint(1, 14)
        }
    }
    
    return product

def generate_dataset(num_products=10000):
    """Generate a dataset with specified number of products"""
    print(f"Generating {num_products} products...")
    
    products = []
    for i in range(1, num_products + 1):
        if i % 1000 == 0:
            print(f"Generated {i} products...")
        products.append(generate_product(i))
    
    return products

def save_as_json(products, filename="products.json"):
    """Save products as JSON file"""
    with open(filename, 'w') as f:
        json.dump(products, f, indent=2)
    print(f"Saved {len(products)} products to {filename}")

def save_as_csv(products, filename="products.csv"):
    """Save products as CSV file"""
    if not products:
        return
    
    # Flatten the nested dictionaries for CSV
    csv_products = []
    for product in products:
        flat_product = product.copy()
        
        # Flatten dimensions
        if 'dimensions' in flat_product:
            flat_product['length_cm'] = flat_product['dimensions']['length_cm']
            flat_product['width_cm'] = flat_product['dimensions']['width_cm']
            flat_product['height_cm'] = flat_product['dimensions']['height_cm']
            del flat_product['dimensions']
        
        # Flatten shipping
        if 'shipping' in flat_product:
            flat_product['free_shipping'] = flat_product['shipping']['free_shipping']
            flat_product['shipping_cost'] = flat_product['shipping']['shipping_cost']
            flat_product['estimated_days'] = flat_product['shipping']['estimated_days']
            del flat_product['shipping']
        
        # Convert tags to string
        flat_product['tags'] = ','.join(flat_product['tags'])
        
        csv_products.append(flat_product)
    
    # Write CSV
    with open(filename, 'w', newline='') as f:
        if csv_products:
            writer = csv.DictWriter(f, fieldnames=csv_products[0].keys())
            writer.writeheader()
            writer.writerows(csv_products)
    
    print(f"Saved {len(products)} products to {filename}")

def main():
    """Main function to generate datasets"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Generate massive product dataset')
    parser.add_argument('--count', type=int, default=10000, help='Number of products to generate')
    parser.add_argument('--format', choices=['json', 'csv', 'both'], default='both', help='Output format')
    parser.add_argument('--output', default='products', help='Output filename prefix')
    
    args = parser.parse_args()
    
    print("🏪 Product Dataset Generator")
    print("=" * 50)
    
    # Generate products
    products = generate_dataset(args.count)
    
    # Save in requested format(s)
    if args.format in ['json', 'both']:
        save_as_json(products, f"{args.output}.json")
    
    if args.format in ['csv', 'both']:
        save_as_csv(products, f"{args.output}.csv")
    
    print("\n✅ Dataset generation complete!")
    print(f"📊 Generated {len(products)} products")
    print(f"📁 Files created in current directory")
    
    # Show sample
    if products:
        print("\n📋 Sample Product:")
        sample = products[0]
        for key, value in list(sample.items())[:10]:
            print(f"  {key}: {value}")
        print("  ...")

if __name__ == "__main__":
    main()