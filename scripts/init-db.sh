#!/bin/bash

# SQLite Database Initialization Script for Cloud Run
# This script ensures the SQLite database file exists before Spring Boot starts
# Spring Session will automatically create the required tables

set -e

DB_PATH="/app/data/awakening-prod.db"
DB_DIR="/app/data"

echo "🗃️ Initializing SQLite database directory..."

# Create database directory if it doesn't exist
mkdir -p "$DB_DIR"

# Create empty database file if it doesn't exist (let Spring Session create tables)
if [ ! -f "$DB_PATH" ]; then
    echo "📝 Creating new SQLite database at $DB_PATH"
    sqlite3 "$DB_PATH" "SELECT 1;" > /dev/null 2>&1
    echo "✅ Empty database file created"
else
    echo "✅ Database file already exists at $DB_PATH"
fi

# Verify database file is accessible
if sqlite3 "$DB_PATH" "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database file is accessible and ready"
else
    echo "❌ Error: Database file is not accessible"
    exit 1
fi

echo "🎉 Database initialization complete - Spring Session will create tables automatically"